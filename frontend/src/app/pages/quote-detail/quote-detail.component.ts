import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { QuoteResponse } from '../../models/quote.model';

@Component({
  selector: 'app-quote-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './quote-detail.component.html',
  styleUrl: './quote-detail.component.css'
})
export class QuoteDetailComponent implements OnInit {
  quote: QuoteResponse | null = null;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private quoteService: QuoteService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // 1. Get quote ID from route parameters
    const idParam = this.route.snapshot.paramMap.get('id');

    if (!idParam || isNaN(Number(idParam))) {
      this.errorMessage = 'Invalid quote ID.';
      return;
    }

    const id = Number(idParam);

    // 2. Load quote from QuoteService with loading & error handling
    this.loading = true;
    this.quoteService.getQuote(id).subscribe({
      next: (quote) => {
        this.quote = quote;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'Failed to load quote.';
        this.loading = false;
      }
    });
  }
}