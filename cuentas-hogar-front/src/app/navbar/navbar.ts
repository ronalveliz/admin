import { Component } from '@angular/core';
import { AuthenticationService } from '../authentication/authentication.service';
import { Router, RouterLink, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {


  title = 'frontend';
  isLoggedin = false;
  userEmail = '';
  isAdmin = false;

  constructor(
    private authService: AuthenticationService,
    private router: Router
    ) {
    this.authService.isLoggedin.subscribe(isLoggedin => this.isLoggedin = isLoggedin);
    this.authService.userEmail.subscribe(userEmail => this.userEmail = userEmail);
    this.authService.isAdmin.subscribe(isAdmin => this.isAdmin = isAdmin);
  }

  logout() {
    this.authService.removeToken();
    this.router.navigate(['/login']);
  }
}
