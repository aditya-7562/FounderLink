import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TeamService } from '../../core/services/team.service';
import { StartupService } from '../../core/services/startup.service';
import { UserService } from '../../core/services/user.service';
import {
  InvitationRequest,
  PaginatedData,
  StartupResponse,
  TeamMemberResponse,
  TeamRole,
  UserResponse
} from '../../models';
import { PaginationControlsComponent } from '../../shared/components/pagination-controls/pagination-controls';

@Component({
  selector: 'app-team',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationControlsComponent],
  templateUrl: './team.html',
  styleUrl: './team.css'
})
export class TeamComponent implements OnInit {
  loading       = signal(true);
  errorMsg      = signal('');
  successMsg    = signal('');

  myStartups        = signal<StartupResponse[]>([]);
  startupPage       = signal<PaginatedData<StartupResponse>>(this.emptyPage<StartupResponse>());
  selectedStartupId = signal<number | null>(null);
  teamMembers       = signal<TeamMemberResponse[]>([]);
  teamPage          = signal<PaginatedData<TeamMemberResponse>>(this.emptyPage<TeamMemberResponse>());
  removing          = signal<number | null>(null);

  showDiscovery    = signal(false);
  allUsers         = signal<UserResponse[]>([]);
  userPage         = signal<PaginatedData<UserResponse>>(this.emptyPage<UserResponse>());
  usersLoading     = signal(false);
  roleFilter       = signal<string>('COFOUNDER');
  searchQuery      = signal('');
  selectedUser     = signal<UserResponse | null>(null);
  selectedRole     = signal<TeamRole | ''>('');
  inviting         = signal(false);

  myTeams       = signal<TeamMemberResponse[]>([]);
  myTeamsPage   = signal<PaginatedData<TeamMemberResponse>>(this.emptyPage<TeamMemberResponse>());
  viewedUser    = signal<UserResponse | null>(null);

  startupNames    = signal<Map<number, string>>(new Map());
  userNames       = signal<Map<number, string>>(new Map());
  startupFounders = signal<Map<number, number>>(new Map());

  readonly teamRoles: TeamRole[] = ['CTO', 'CPO', 'MARKETING_HEAD', 'ENGINEERING_LEAD'];
  readonly roleLabels: Record<TeamRole, string> = {
    CTO: 'Chief Technology Officer',
    CPO: 'Chief Product Officer',
    MARKETING_HEAD: 'Marketing Head',
    ENGINEERING_LEAD: 'Engineering Lead'
  };
  readonly roleShort: Record<TeamRole, string> = {
    CTO: 'CTO', CPO: 'CPO', MARKETING_HEAD: 'Mktg Head', ENGINEERING_LEAD: 'Eng Lead'
  };

  private hasRole(r: string): boolean {
    const stored = this.authService.role() ?? '';
    return stored === r || stored === `ROLE_${r}`;
  }
  isFounder(): boolean { return this.hasRole('FOUNDER'); }
  isCoFounder(): boolean { return this.hasRole('COFOUNDER'); }

  constructor(
    public authService: AuthService,
    private teamService: TeamService,
    private startupService: StartupService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.isFounder()) this.loadFounderData();
    if (this.isCoFounder()) this.loadCoFounderData();

    this.startupService.getAll({ page: 0, size: 50, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const nameMap = new Map<number, string>();
        const founderMap = new Map<number, number>();
        env.data?.content.forEach(s => {
          nameMap.set(s.id, s.name);
          founderMap.set(s.id, s.founderId);
        });
        this.startupNames.set(nameMap);
        this.startupFounders.set(founderMap);
      }
    });

    this.userService.getAllUsers({ page: 0, size: 50, sort: 'id,asc' }).subscribe({
      next: env => {
        const map = new Map<number, string>();
        env.data?.content.forEach(u => map.set(u.userId, u.name || u.email));
        this.userNames.set(map);
      }
    });
  }

  loadFounderData(page = 0): void {
    this.startupService.getMyStartups({ page, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const startupPage = env.data ?? this.startupPage();
        this.startupPage.set(startupPage);
        this.myStartups.set(startupPage.content);
        if (startupPage.content.length) {
          const current = this.selectedStartupId();
          const selected = startupPage.content.find(s => s.id === current) ?? startupPage.content[0];
          this.selectedStartupId.set(selected.id);
          this.loadTeam(selected.id, 0);
        } else {
          this.teamMembers.set([]);
          this.teamPage.set(this.emptyPage<TeamMemberResponse>());
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false)
    });
  }

  loadTeam(startupId: number, page = 0): void {
    this.loading.set(true);
    this.teamService.getTeamMembers(startupId, { page, size: 10, sort: 'joinedAt,desc' }).subscribe({
      next: env => {
        const teamPage = env.data ?? this.teamPage();
        this.teamPage.set(teamPage);
        this.teamMembers.set(teamPage.content);
        this.loading.set(false);
      },
      error: env => {
        this.errorMsg.set(env.error ?? 'Failed to load team.');
        this.loading.set(false);
      }
    });
  }

  onStartupChange(id: number): void {
    this.selectedStartupId.set(id);
    this.closeDiscovery();
    this.loadTeam(id, 0);
  }

  removeMember(memberId: number): void {
    if (!confirm('Remove this team member?')) return;
    this.removing.set(memberId);
    this.teamService.removeMember(memberId).subscribe({
      next: () => {
        this.removing.set(null);
        this.teamMembers.update(list => list.filter(m => m.id !== memberId));
        this.successMsg.set('Member removed.');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: env => {
        this.removing.set(null);
        this.errorMsg.set(env.error ?? 'Failed to remove member.');
      }
    });
  }

  messageMember(userId: number): void {
    this.router.navigate(['/dashboard/messages'], { queryParams: { user: userId } });
  }

  openDiscovery(): void {
    this.showDiscovery.set(true);
    this.selectedUser.set(null);
    this.selectedRole.set('');
    this.searchQuery.set('');
    this.loadUsersForRole('COFOUNDER', 0);
  }

  closeDiscovery(): void {
    this.showDiscovery.set(false);
    this.selectedUser.set(null);
    this.selectedRole.set('');
    this.viewedUser.set(null);
  }

  viewProfile(u: UserResponse): void {
    this.viewedUser.set(u);
  }

  closeProfile(): void {
    this.viewedUser.set(null);
  }

  loadUsersForRole(role: string, page = 0): void {
    this.roleFilter.set(role);
    this.usersLoading.set(true);
    this.selectedUser.set(null);
    const obs$ = role
      ? this.userService.getUsersByRole(role, { page, size: 10, sort: 'id,asc' })
      : this.userService.getAllUsers({ page, size: 10, sort: 'id,asc' });

    obs$.subscribe({
      next: env => {
        const userPage = env.data ?? this.userPage();
        this.userPage.set(userPage);
        this.allUsers.set(userPage.content);
        this.usersLoading.set(false);
      },
      error: () => {
        this.allUsers.set([]);
        this.usersLoading.set(false);
      }
    });
  }

  filteredUsers = computed(() => {
    const q = this.searchQuery().toLowerCase();
    const memberIds = new Set(this.teamMembers().map(m => m.userId));
    const myId = this.authService.userId();
    return this.allUsers().filter(u =>
      u.userId !== myId &&
      !memberIds.has(u.userId) &&
      (!q || (u.name ?? '').toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        (u.skills ?? '').toLowerCase().includes(q))
    );
  });

  selectUserToInvite(user: UserResponse): void {
    this.selectedUser.set(user);
    this.selectedRole.set('');
  }

  sendInvite(): void {
    const user = this.selectedUser();
    const role = this.selectedRole();
    const startupId = this.selectedStartupId();

    if (!user || !role || !startupId) {
      this.errorMsg.set('Please select a user and a role.');
      return;
    }

    this.inviting.set(true);
    this.errorMsg.set('');

    const payload: InvitationRequest = {
      startupId,
      invitedUserId: user.userId,
      role
    };

    this.teamService.sendInvitation(payload).subscribe({
      next: () => {
        this.inviting.set(false);
        this.closeDiscovery();
        this.successMsg.set(`Invitation sent to ${user.name ?? user.email}!`);
        setTimeout(() => this.successMsg.set(''), 4000);
      },
      error: env => {
        this.inviting.set(false);
        this.errorMsg.set(env.error ?? 'Failed to send invitation.');
      }
    });
  }

  loadCoFounderData(page = 0): void {
    this.teamService.getMyActiveRoles({ page, size: 10, sort: 'joinedAt,desc' }).subscribe({
      next: env => {
        const myTeamsPage = env.data ?? this.myTeamsPage();
        this.myTeamsPage.set(myTeamsPage);
        this.myTeams.set(myTeamsPage.content);
        this.loading.set(false);
      },
      error: env => {
        this.errorMsg.set(env.error ?? 'Failed to load teams.');
        this.loading.set(false);
      }
    });
  }

  viewStartup(startupId: number): void {
    this.router.navigate(['/startup', startupId]);
  }

  messageFounder(startupId: number): void {
    const founderId = this.startupFounders().get(startupId);
    if (founderId) {
      this.router.navigate(['/dashboard/messages'], { queryParams: { user: founderId } });
    }
  }

  roleLabel(role: string): string {
    return this.roleLabels[role as TeamRole] ?? role.replace(/_/g, ' ');
  }

  roleShortLabel(role: string): string {
    return this.roleShort[role as TeamRole] ?? role;
  }

  roleClass(role: string): string {
    return role === 'CTO' ? 'badge-purple'
      : role === 'CPO' ? 'badge-info'
        : role === 'MARKETING_HEAD' ? 'badge-warning'
          : 'badge-success';
  }

  userInitials(user: UserResponse): string {
    const name = user.name ?? user.email;
    return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  nextStartupPage(): void {
    this.loadFounderData(this.startupPage().page + 1);
  }

  previousStartupPage(): void {
    this.loadFounderData(Math.max(this.startupPage().page - 1, 0));
  }

  nextTeamPage(): void {
    const startupId = this.selectedStartupId();
    if (startupId == null) return;
    this.loadTeam(startupId, this.teamPage().page + 1);
  }

  previousTeamPage(): void {
    const startupId = this.selectedStartupId();
    if (startupId == null) return;
    this.loadTeam(startupId, Math.max(this.teamPage().page - 1, 0));
  }

  nextUserPage(): void {
    this.loadUsersForRole(this.roleFilter(), this.userPage().page + 1);
  }

  previousUserPage(): void {
    this.loadUsersForRole(this.roleFilter(), Math.max(this.userPage().page - 1, 0));
  }

  nextMyTeamsPage(): void {
    this.loadCoFounderData(this.myTeamsPage().page + 1);
  }

  previousMyTeamsPage(): void {
    this.loadCoFounderData(Math.max(this.myTeamsPage().page - 1, 0));
  }

  private emptyPage<T>(): PaginatedData<T> {
    return {
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true
    };
  }
}
