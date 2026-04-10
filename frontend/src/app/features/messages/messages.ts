import { Component, OnInit, signal, OnDestroy, ViewChild, ElementRef, AfterViewChecked, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { MessagingService } from '../../core/services/messaging.service';
import { UserService } from '../../core/services/user.service';
import { MessageResponse, PaginatedData, UserResponse } from '../../models';
import { PaginationControlsComponent } from '../../shared/components/pagination-controls/pagination-controls';
import { Client, IMessage } from '@stomp/stompjs';
import * as SockJSAmbient from 'sockjs-client';
import { environment } from '../../../environments/environment';

// Handling typical imports for CommonJS modules in Angular without esModuleInterop
const SockJS = (SockJSAmbient as any).default || SockJSAmbient;

interface ConversationPartner {
  userId: number;
  name: string;
  email: string;
  lastMessage: string;
  lastMessageTime: string;
}

@Component({
  selector: 'app-messages',
  imports: [CommonModule, FormsModule, PaginationControlsComponent],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class MessagesComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  
  partners       = signal<ConversationPartner[]>([]);
  partnerPage    = signal<PaginatedData<number>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  selectedPartner = signal<ConversationPartner | null>(null);
  
  // Chat state
  messages       = signal<MessageResponse[]>([]);
  seenIds        = new Set<number>();
  oldestCursor   = signal<number | null>(null);
  newestCursor   = signal<number | null>(null);

  allUsers       = signal<UserResponse[]>([]);
  loading        = signal(true);
  loadingOlder   = signal(false);
  sendingMessage = signal(false);
  showUserSelector = signal(false);
  errorMsg       = signal('');
  successMsg     = signal('');
  messageContent = '';
  
  private stompClient: Client | null = null;
  private currentSubscription: any = null;
  private shouldScrollToBottom = false;

  constructor(
    public authService: AuthService,
    private messagingService: MessagingService,
    private userService: UserService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const targetUserId = this.route.snapshot.queryParamMap.get('user');
    this.loadConversations(targetUserId ? Number(targetUserId) : undefined);
    this.initializeWebSocket();
  }

  ngOnDestroy(): void {
    this.disconnectWebSocket();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    } catch(err) { }
  }

  // --- WebSocket Connection ---

  private initializeWebSocket(): void {
    const wsUrl = environment.apiUrl.replace(/\/+$/, '') + '/ws';
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      debug: (str) => {
        // console.log(str);
      }
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected to WS');
      if (this.selectedPartner()) {
        this.subscribeToConversation(this.selectedPartner()!.userId);
        this.catchUpMessages(this.selectedPartner()!.userId);
      }
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    this.stompClient.activate();
  }

  private disconnectWebSocket(): void {
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
    }
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.deactivate();
    }
  }

  private subscribeToConversation(partnerId: number): void {
    if (!this.stompClient || !this.stompClient.connected) return;
    
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
    }

    const currentUserId = this.authService.userId()!;
    const lo = Math.min(currentUserId, partnerId);
    const hi = Math.max(currentUserId, partnerId);
    const topic = `/topic/conversation/${lo}/${hi}`;

    this.currentSubscription = this.stompClient.subscribe(topic, (message: IMessage) => {
      const msgData: MessageResponse = JSON.parse(message.body);
      this.handleIncomingMessage(msgData);
    });
  }

  private handleIncomingMessage(msg: MessageResponse): void {
    if (this.seenIds.has(msg.id)) return;
    this.seenIds.add(msg.id);

    this.messages.update(msgs => {
      const newMsgs = [...msgs];
      let i = newMsgs.length - 1;
      while (i >= 0 && newMsgs[i].id > msg.id) {
        i--;
      }
      newMsgs.splice(i + 1, 0, msg);
      return newMsgs;
    });

    const newest = this.newestCursor() ?? 0;
    this.newestCursor.set(Math.max(newest, msg.id));
    this.shouldScrollToBottom = true;
  }

  // --- End WebSocket ---

  loadConversations(targetUserId?: number): void {
    this.loading.set(true);
    this.messagingService.getPartnerIds({ page: 0, size: 10 }).subscribe({
      next: env => {
        const partnerPage = env.data ?? this.partnerPage();
        this.partnerPage.set(partnerPage);
        const ids = partnerPage.content;
        if (ids.length === 0 && !targetUserId) { this.loading.set(false); return; }
        
        // Fetch user details for each partner id
        const fetches = ids.map(id =>
          new Promise<UserResponse | null>(resolve => {
            this.userService.getUser(id).subscribe({
              next: uenv => resolve(uenv.data),
              error: () => resolve(null)
            });
          })
        );
        
        Promise.all(fetches).then(users => {
          const validPartners: ConversationPartner[] = users
            .filter((u): u is UserResponse => u !== null)
            .map(u => ({ userId: u.userId, name: u.name ?? u.email, email: u.email, lastMessage: '', lastMessageTime: '' }));
          this.partners.set(validPartners);
          
          if (targetUserId) {
            const existing = validPartners.find(p => p.userId === targetUserId);
            if (existing) {
              this.selectPartner(existing);
              this.loading.set(false);
            } else {
              this.userService.getUser(targetUserId).subscribe({
                next: (uenv) => {
                  if (uenv.data) this.startConversationWith(uenv.data);
                  this.loading.set(false);
                },
                error: () => this.loading.set(false)
              });
            }
          } else if (validPartners.length > 0) {
            this.selectPartner(validPartners[0]);
            this.loading.set(false);
          } else {
            this.loading.set(false);
          }
        });
      },
      error: () => { this.errorMsg.set('Failed to load conversations.'); this.loading.set(false); }
    });
  }

  selectPartner(partner: ConversationPartner): void {
    this.selectedPartner.set(partner);
    this.messageContent = '';
    this.showUserSelector.set(false);
    
    // Clear chat state
    this.messages.set([]);
    this.seenIds.clear();
    this.oldestCursor.set(null);
    this.newestCursor.set(null);

    // Subscribe to new conversation
    this.subscribeToConversation(partner.userId);
    this.loadInitialMessages(partner.userId);
  }

  loadInitialMessages(partnerId: number): void {
    this.messagingService.getConversationCursor(partnerId, { size: 20 }).subscribe({
      next: env => {
        if (env.data) {
          env.data.content.forEach(msg => this.seenIds.add(msg.id));
          this.messages.set(env.data.content);
          this.oldestCursor.set(env.data.nextCursor);
          this.newestCursor.set(env.data.prevCursor);
          this.shouldScrollToBottom = true;
        }
      },
      error: () => this.errorMsg.set('Failed to load messages.')
    });
  }

  loadOlderMessages(): void {
    const partnerId = this.selectedPartner()?.userId;
    const beforeId = this.oldestCursor();
    if (!partnerId || !beforeId) return;

    this.loadingOlder.set(true);
    this.messagingService.getConversationCursor(partnerId, { size: 20, before: beforeId }).subscribe({
      next: env => {
        this.loadingOlder.set(false);
        if (env.data) {
          const newMessages = env.data.content.filter(msg => !this.seenIds.has(msg.id));
          newMessages.forEach(msg => this.seenIds.add(msg.id));
          
          this.messages.update(msgs => [...newMessages, ...msgs]);
          this.oldestCursor.set(env.data.nextCursor);
        }
      },
      error: () => {
        this.loadingOlder.set(false);
        this.errorMsg.set('Failed to load older messages.');
      }
    });
  }

  catchUpMessages(partnerId: number): void {
    const afterId = this.newestCursor();
    if (!afterId) return;

    this.messagingService.getConversationCursor(partnerId, { size: 50, after: afterId }).subscribe({
      next: env => {
        if (env.data) {
          const missedMessages = env.data.content.filter(msg => !this.seenIds.has(msg.id));
          missedMessages.forEach(msg => {
            this.seenIds.add(msg.id);
            this.messages.update(msgs => {
              const newMsgs = [...msgs];
              let i = newMsgs.length - 1;
              while (i >= 0 && newMsgs[i].id > msg.id) {
                i--;
              }
              newMsgs.splice(i + 1, 0, msg);
              return newMsgs;
            });
          });
          
          if (env.data.prevCursor) {
            this.newestCursor.set(Math.max(this.newestCursor() || 0, env.data.prevCursor));
          }
          if (missedMessages.length > 0) {
            this.shouldScrollToBottom = true;
          }
        }
      }
    });
  }

  sendMessage(): void {
    const content = this.messageContent.trim();
    const partner = this.selectedPartner();
    if (!content) { this.errorMsg.set('Message cannot be empty.'); return; }
    if (!partner)  { this.errorMsg.set('Please select a conversation.'); return; }

    this.sendingMessage.set(true);
    this.errorMsg.set('');

    this.messagingService.sendMessage(partner.userId, content).subscribe({
      next: env => {
        this.messageContent = '';
        this.sendingMessage.set(false);
        if (env.data) {
          this.handleIncomingMessage(env.data);
        }
      },
      error: env => {
        this.sendingMessage.set(false);
        this.errorMsg.set(env.error ?? 'Failed to send message.');
      }
    });
  }

  loadAllUsers(): void {
    if (this.allUsers().length === 0) {
      const currentUserId = this.authService.userId();
      this.userService.getAllUsers({ page: 0, size: 50, sort: 'id,asc' }).subscribe({
        next: env => {
          this.allUsers.set((env.data?.content ?? []).filter(u => u.userId !== currentUserId));
        }
      });
    }
    this.showUserSelector.update(v => !v);
  }

  startConversationWith(user: UserResponse): void {
    const partner: ConversationPartner = {
      userId: user.userId, name: user.name ?? user.email, email: user.email,
      lastMessage: '', lastMessageTime: ''
    };
    // Add to partners list if not already present
    const exists = this.partners().some(p => p.userId === user.userId);
    if (!exists) {
      this.partners.update(list => [partner, ...list]);
    }
    this.selectPartner(partner);
  }

  isCurrentUser(senderId: number): boolean {
    return senderId === this.authService.userId();
  }

  formatTime(date: string): string {
    return new Date(date).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  nextPartnerPage(): void {
    this.loadPartnerPage(this.partnerPage().page + 1);
  }

  previousPartnerPage(): void {
    this.loadPartnerPage(Math.max(this.partnerPage().page - 1, 0));
  }

  private loadPartnerPage(page: number): void {
    this.loading.set(true);
    this.messagingService.getPartnerIds({ page, size: 10 }).subscribe({
      next: env => {
        const targetUserId = this.selectedPartner()?.userId;
        const partnerPage = env.data ?? this.partnerPage();
        this.partnerPage.set(partnerPage);
        const ids = partnerPage.content;
        if (ids.length === 0) {
          this.partners.set([]);
          this.loading.set(false);
          return;
        }

        const fetches = ids.map(id =>
          new Promise<UserResponse | null>(resolve => {
            this.userService.getUser(id).subscribe({
              next: uenv => resolve(uenv.data),
              error: () => resolve(null)
            });
          })
        );

        Promise.all(fetches).then(users => {
          const validPartners: ConversationPartner[] = users
            .filter((u): u is UserResponse => u !== null)
            .map(u => ({ userId: u.userId, name: u.name ?? u.email, email: u.email, lastMessage: '', lastMessageTime: '' }));
          this.partners.set(validPartners);

          if (targetUserId) {
            const existing = validPartners.find(p => p.userId === targetUserId);
            if (existing) {
              this.selectedPartner.set(existing);
            }
          }
          this.loading.set(false);
        });
      },
      error: () => { this.errorMsg.set('Failed to load conversations.'); this.loading.set(false); }
    });
  }
}
