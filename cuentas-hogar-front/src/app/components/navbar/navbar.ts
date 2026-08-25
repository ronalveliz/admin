import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth-service';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  title = 'frontend';
  isLoggedin = false;
  userEmail = '';
  isAdmin = false;

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.isLoggedin = !!user;
      this.userEmail = user?.usuario?.email ?? '';
      this.isAdmin = this.authService.isAdmin();
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}