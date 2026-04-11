import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex h-screen bg-neutral-900 text-white font-sans overflow-hidden">
      <!-- Sidebar -->
      <aside class="w-64 bg-neutral-950 border-r border-neutral-800 flex flex-col items-center py-8 z-10 shrink-0">
        <h1 class="text-2xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent mb-12">
          Admin Portal
        </h1>
        
        <nav class="flex flex-col w-full px-4 gap-2">
          <a routerLink="/admin/dashboard" routerLinkActive="bg-neutral-800 text-white border-l-4 border-blue-500" 
             class="flex items-center gap-3 px-4 py-3 rounded-md text-neutral-400 hover:bg-neutral-800 hover:text-white transition-all">
            <span class="material-icons text-xl">dashboard</span> Dashboard
          </a>
          
          <a routerLink="/admin/users" routerLinkActive="bg-neutral-800 text-white border-l-4 border-blue-500" 
             class="flex items-center gap-3 px-4 py-3 rounded-md text-neutral-400 hover:bg-neutral-800 hover:text-white transition-all">
            <span class="material-icons text-xl">people</span> Users
          </a>
          
          <a routerLink="/admin/startups" routerLinkActive="bg-neutral-800 text-white border-l-4 border-blue-500" 
             class="flex items-center gap-3 px-4 py-3 rounded-md text-neutral-400 hover:bg-neutral-800 hover:text-white transition-all">
            <span class="material-icons text-xl">rocket_launch</span> Startups
          </a>
        </nav>

        <div class="mt-auto w-full px-4">
          <button (click)="exitAdmin()" class="flex items-center gap-3 w-full px-4 py-3 rounded-md text-neutral-400 hover:bg-red-500/10 hover:text-red-400 transition-all">
            <span class="material-icons text-xl">logout</span> Exit Admin
          </button>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="flex-1 h-full overflow-y-auto bg-neutral-900 custom-scrollbar">
        <div class="p-8 max-w-7xl mx-auto min-h-full">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .custom-scrollbar::-webkit-scrollbar { width: 8px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: #171717; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #262626; border-radius: 4px; }
    .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #404040; }
  `]
})
export class AdminLayoutComponent {
  router = inject(Router);

  exitAdmin() {
    this.router.navigate(['/dashboard']);
  }
}
