import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, ModerationRequest } from '../services/admin';

@Component({
  selector: 'app-admin-startups',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div class="mb-8 border-b border-neutral-800 pb-4">
        <h2 class="text-3xl font-bold tracking-tight text-white">Startups Moderation</h2>
        <p class="text-neutral-400 mt-2">Review, approve, and manage startup listings.</p>
      </div>

      <!-- Table Section -->
      <div class="bg-neutral-950 border border-neutral-800 rounded-xl overflow-hidden shadow-xl shadow-black/40">
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-neutral-900 border-b border-neutral-800 text-neutral-400 text-sm">
                <th class="px-6 py-4 font-semibold">Startup Info</th>
                <th class="px-6 py-4 font-semibold">Industry / Stage</th>
                <th class="px-6 py-4 font-semibold">Moderation</th>
                <th class="px-6 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-neutral-800/60">
              <tr *ngFor="let startup of startups" class="hover:bg-neutral-900/50 transition-colors">
                <td class="px-6 py-4">
                  <div class="font-medium text-white">{{ startup.name }}</div>
                  <div class="text-xs text-neutral-500 line-clamp-1 mt-1 max-w-xs" [title]="startup.description">{{ startup.description }}</div>
                </td>
                <td class="px-6 py-4">
                  <div class="text-sm text-neutral-300">{{ startup.industry }}</div>
                  <span class="px-2.5 py-0.5 mt-1 inline-block rounded border border-neutral-700 bg-neutral-800/50 text-[10px] text-neutral-400 uppercase tracking-wider">
                    {{ startup.stage }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 rounded-md text-xs font-medium tracking-wide flex items-center justify-center w-max gap-1"
                        [ngClass]="{
                          'bg-yellow-500/10 text-yellow-500 border border-yellow-500/20': startup.moderationStatus === 'PENDING_REVIEW',
                          'bg-green-500/10 text-green-400 border border-green-500/20': startup.moderationStatus === 'APPROVED' || !startup.moderationStatus,
                          'bg-red-500/10 text-red-500 border border-red-500/20': startup.moderationStatus === 'REJECTED',
                          'bg-orange-500/10 text-orange-400 border border-orange-500/20': startup.moderationStatus === 'FLAGGED'
                        }">
                    <span class="material-icons text-[14px]">
                      {{ 
                        startup.moderationStatus === 'PENDING_REVIEW' ? 'pending_actions' :
                        startup.moderationStatus === 'REJECTED' ? 'cancel' :
                        startup.moderationStatus === 'FLAGGED' ? 'flag' : 'check_circle'
                      }}
                    </span>
                    {{ startup.moderationStatus || 'APPROVED' }}
                  </span>
                  <div *ngIf="startup.moderationReason" class="text-[10px] text-neutral-500 mt-1 limit-text max-w-xs truncate" [title]="startup.moderationReason">
                    {{ startup.moderationReason }}
                  </div>
                </td>
                <td class="px-6 py-4 text-right">
                  <div class="flex justify-end gap-2">
                    <button *ngIf="startup.moderationStatus !== 'APPROVED'" (click)="updateStatus(startup.id, 'APPROVED')" title="Approve Startup"
                           class="p-2 text-green-400 hover:bg-green-400/10 rounded-md transition-colors">
                      <span class="material-icons text-sm">check</span>
                    </button>
                    <button *ngIf="startup.moderationStatus !== 'FLAGGED'" (click)="promptReason(startup.id, 'FLAGGED')" title="Flag Startup"
                           class="p-2 text-orange-400 hover:bg-orange-400/10 rounded-md transition-colors">
                      <span class="material-icons text-sm">flag</span>
                    </button>
                    <button *ngIf="startup.moderationStatus !== 'REJECTED'" (click)="promptReason(startup.id, 'REJECTED')" title="Reject Startup"
                           class="p-2 text-red-400 hover:bg-red-400/10 rounded-md transition-colors">
                      <span class="material-icons text-sm">close</span>
                    </button>
                    <button (click)="forceDelete(startup.id)" title="Force Delete (Permanent)"
                           class="p-2 text-neutral-500 hover:bg-red-500 hover:text-white rounded-md transition-colors ml-4">
                      <span class="material-icons text-sm">delete_forever</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="startups.length === 0">
                <td colspan="4" class="px-6 py-12 text-center text-neutral-500">
                  <span class="material-icons text-4xl block mb-2 opacity-50">inbox</span>
                  No startups available.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <!-- Pagination Component -->
        <div class="bg-neutral-900 border-t border-neutral-800 p-4 flex justify-between items-center text-sm text-neutral-400" *ngIf="totalPages > 1">
          <div>Page {{ page + 1 }} of {{ totalPages }}</div>
          <div class="flex gap-2">
            <button [disabled]="page === 0" (click)="changePage(page - 1)" class="px-3 py-1 bg-neutral-800 rounded hover:bg-neutral-700 disabled:opacity-50">Previous</button>
            <button [disabled]="page >= totalPages - 1" (click)="changePage(page + 1)" class="px-3 py-1 bg-neutral-800 rounded hover:bg-neutral-700 disabled:opacity-50">Next</button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class StartupsComponent implements OnInit {
  private adminService = inject(AdminService);
  
  startups: any[] = [];
  page = 0;
  size = 10;
  totalPages = 0;

  ngOnInit() {
    this.loadStartups();
  }

  loadStartups() {
    this.adminService.getStartups(this.page, this.size).subscribe({
      next: (res) => {
        // Handle varying pagination shapes
        const pageData = res.data || res;
        this.startups = pageData.content || [];
        this.totalPages = pageData.totalPages || 0;
      },
      error: (err) => console.error(err)
    });
  }

  changePage(newPage: number) {
    this.page = newPage;
    this.loadStartups();
  }

  updateStatus(id: number, status: 'PENDING_REVIEW'|'APPROVED'|'REJECTED'|'FLAGGED', reason?: string) {
    if(confirm(`Are you sure you want to mark this startup as ${status}?`)) {
      this.adminService.updateStartupModeration(id, { status, reason }).subscribe({
        next: () => this.loadStartups(),
        error: (err) => alert('Failed to update startup status')
      });
    }
  }

  promptReason(id: number, status: 'REJECTED'|'FLAGGED') {
    const reason = prompt(`Please provide a reason for marking as ${status}:`);
    if (reason !== null) {
      this.updateStatus(id, status, reason);
    }
  }

  forceDelete(id: number) {
    if(confirm('WARNING: Force deleting a startup bypasses founder checks. Are you sure?')) {
      this.adminService.deleteStartup(id).subscribe({
        next: () => this.loadStartups(),
        error: (err) => alert('Failed to delete startup')
      });
    }
  }
}
