import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { TeamService } from '../../../core/services/team.service';
import { StartupService } from '../../../core/services/startup.service';
import { ConfirmService } from '../../../core/services/confirm.service';
import { InvitationResponse, InvitationStatus, PaginatedData } from '../../../models';
import { PaginationControlsComponent } from '../../../shared/components/pagination-controls/pagination-controls';

@Component({
  selector: 'app-invitations',
  imports: [CommonModule, PaginationControlsComponent],
  templateUrl: './invitations.html',
  styleUrl: './invitations.css'
})
export class InvitationsComponent implements OnInit {
  invitations = signal<InvitationResponse[]>([]);
  invitationPage = signal<PaginatedData<InvitationResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  loading     = signal(true);
  acting      = signal<number | null>(null);
  errorMsg    = signal('');
  successMsg  = signal('');
  startupNames = signal<Map<number, string>>(new Map());

  constructor(
    public authService: AuthService,
    private teamService: TeamService,
    private startupService: StartupService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit(): void { this.loadInvitations(); }

  loadInvitations(page = 0): void {
    this.loading.set(true);
    this.teamService.getMyInvitations({ page, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const invitationPage = env.data ?? this.invitationPage();
        this.invitationPage.set(invitationPage);
        this.invitations.set(invitationPage.content);
        this.loading.set(false);
      },
      error: env => { this.errorMsg.set(env.error ?? 'Failed to load invitations.'); this.loading.set(false); }
    });

    this.startupService.getAll({ page: 0, size: 50, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const map = new Map<number, string>();
        env.data?.content.forEach(s => map.set(s.id, s.name));
        this.startupNames.set(map);
      }
    });
  }

  accept(invitation: InvitationResponse): void {
    this.acting.set(invitation.id);
    this.errorMsg.set('');
    this.teamService.joinTeam({ invitationId: invitation.id }).subscribe({
      next: () => {
        this.acting.set(null);
        this.invitations.update(list =>
          list.map(i => i.id === invitation.id ? { ...i, status: 'ACCEPTED' as InvitationStatus } : i)
        );
        this.successMsg.set('You have joined the team!');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: env => {
        this.acting.set(null);
        this.errorMsg.set(env.error ?? 'Failed to accept invitation.');
      }
    });
  }

  async reject(invitation: InvitationResponse): Promise<void> {
    const confirmed = await this.confirmService.confirm('Are you sure you want to reject this invitation?', { isDestructive: true });
    if (!confirmed) return;
    this.acting.set(invitation.id);
    this.errorMsg.set('');
    this.teamService.rejectInvitation(invitation.id).subscribe({
      next: () => {
        this.acting.set(null);
        this.invitations.update(list =>
          list.map(i => i.id === invitation.id ? { ...i, status: 'REJECTED' as InvitationStatus } : i)
        );
        this.successMsg.set('Invitation rejected.');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: env => {
        this.acting.set(null);
        this.errorMsg.set(env.error ?? 'Failed to reject invitation.');
      }
    });
  }

  pending():   InvitationResponse[] { return this.invitations().filter(i => i.status === 'PENDING'); }
  responded(): InvitationResponse[] { return this.invitations().filter(i => i.status !== 'PENDING'); }

  statusLabel(status: InvitationStatus): string {
    const labels: Record<InvitationStatus, string> = {
      PENDING: 'Awaiting Response', ACCEPTED: 'Accepted',
      REJECTED: 'Rejected', CANCELLED: 'Cancelled'
    };
    return labels[status] ?? status;
  }

  statusClass(status: string): string {
    return status === 'ACCEPTED'  ? 'badge-success'
         : status === 'PENDING'   ? 'badge-warning'
         : status === 'REJECTED'  ? 'badge-danger'
         : 'badge-gray';
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      CTO: 'CTO', CPO: 'CPO', MARKETING_HEAD: 'Marketing Head', ENGINEERING_LEAD: 'Engineering Lead'
    };
    return labels[role] ?? role.replace(/_/g, ' ');
  }

  roleShortLabel(role: string): string {
    const labels: Record<string, string> = {
      CTO: 'CTO', CPO: 'CPO', MARKETING_HEAD: 'MH', ENGINEERING_LEAD: 'EL', CO_FOUNDER: 'CF'
    };
    return labels[role] ?? role.split('_').map(w => w[0]).join('').toUpperCase();
  }

  nextPage(): void {
    this.loadInvitations(this.invitationPage().page + 1);
  }

  previousPage(): void {
    this.loadInvitations(Math.max(this.invitationPage().page - 1, 0));
  }
}
