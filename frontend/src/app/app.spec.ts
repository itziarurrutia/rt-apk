import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('muestra la navegación de equipos y tickets', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const links = (fixture.nativeElement as HTMLElement).querySelectorAll('.top-nav a');
    expect(Array.from(links).map((a) => a.textContent?.trim())).toEqual([
      'computer Equipos',
      'schedule Tickets recientes',
      'confirmation_number Mis tickets',
    ]);
  });
});
