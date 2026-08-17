import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatIconModule],
  template: `
    <mat-toolbar class="app-toolbar">
      <span class="brand">
        <mat-icon>payments</mat-icon>
        ACME&nbsp;<strong>Salary</strong>
      </span>
      <nav>
        <a routerLink="/employees" routerLinkActive="active">
          <mat-icon>groups</mat-icon>
          Employees
        </a>
        <a routerLink="/insights" routerLinkActive="active">
          <mat-icon>insights</mat-icon>
          Insights
        </a>
      </nav>
    </mat-toolbar>
    <main class="app-main">
      <router-outlet />
    </main>
  `,
  styles: `
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 10;
      gap: 32px;
      background: #1a237e;
      color: #fff;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      letter-spacing: 0.2px;
    }
    nav {
      display: flex;
      gap: 8px;
    }
    nav a {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 20px;
      color: rgba(255, 255, 255, 0.85);
      text-decoration: none;
      font-size: 14px;
    }
    nav a.active {
      background: rgba(255, 255, 255, 0.16);
      color: #fff;
    }
    .app-main {
      max-width: 1280px;
      margin: 0 auto;
      padding: 24px;
    }
  `,
})
export class AppComponent {}
