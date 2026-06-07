import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';
import { userLoggedIntGuard } from './user-logged-int-guard';

describe('userLoggedIntGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => userLoggedIntGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
