import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { QuoteService } from '../../services/quote.service';
import { ProductService } from '../../services/product.service';
import { QuoteResponse } from '../../models/quote.model';
import { Product } from '../../models/product.model';

/**
 * Component for displaying a list of all quotes with filtering and sorting
 */
@Component({
  selector: 'app-quote-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './quote-list.component.html',
  styleUrl: './quote-list.component.css'
})
export class QuoteListComponent implements OnInit {
  quotes: QuoteResponse[] = [];
  filteredQuotes: QuoteResponse[] = [];
  products: Product[] = [];
  loading = false;
  errorMessage: string | null = null;

  // Filter state
  selectedProductId: number | null = null;
  minPrice: number | null = null;

  // Sort state
  sortField: 'date' | 'price' = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private quoteService: QuoteService,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Load products for the filter dropdown (errors are silent — list still works)
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
      },
      error: () => {
        // Non-critical: filter dropdown will just be empty
      }
    });

    // Load all quotes on init
    this.loadQuotes();
  }

  applyFilters(): void {
    this.loadQuotes();
  }

  /**
   * Reset all filters back to defaults and reload the full quote list
   */
  resetFilters(): void {
    this.selectedProductId = null;
    this.minPrice = null;
    this.loadQuotes();
  }

  changeSortField(field: 'date' | 'price'): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortQuotes();
  }

  /**
   * Navigate to the detail page of a specific quote.
   */
  viewQuote(id: number): void {
    this.router.navigate(['/quotes', id]);
  }

  private loadQuotes(): void {
    this.loading = true;
    this.errorMessage = null;

    // Only include filter params that have an actual value
    const filters: { productId?: number; minPrice?: number } = {};
    if (this.selectedProductId !== null) filters.productId = this.selectedProductId;
    if (this.minPrice !== null)          filters.minPrice  = this.minPrice;

    this.quoteService.getQuotes(filters).subscribe({
      next: (quotes) => {
        this.quotes = quotes;
        this.filteredQuotes = [...quotes];
        this.sortQuotes();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'Failed to load quotes.';
        this.loading = false;
      }
    });
  }

  private sortQuotes(): void {
    this.filteredQuotes.sort((a, b) => {
      let comparison = 0;

      if (this.sortField === 'date') {
        // ISO strings can be compared lexicographically
        comparison = a.createdAt.localeCompare(b.createdAt);
      } else if (this.sortField === 'price') {
        comparison = a.finalPrice - b.finalPrice;
      }

      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }
}