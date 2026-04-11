import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { ProductService } from '../../services/product.service';
import { Product } from '../../models/product.model';

const ZONES = [
  { code: 'TUN', name: 'Grand Tunis' },
  { code: 'SFX', name: 'Sfax' },
  { code: 'SOU', name: 'Sousse' }
];

/**
 * Component for creating a new insurance quote.
 * Loads available products from the API, presents a reactive form,
 * and submits a quoteRequest to the backend on valid submission.
 */
@Component({
  selector: 'app-quote-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './quote-form.component.html',
  styleUrl: './quote-form.component.css'
})
export class QuoteFormComponent implements OnInit {
  form: FormGroup;
  products: Product[] = [];
  zones = ZONES;
  loading = false;
  submitted = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private quoteService: QuoteService,
    private productService: ProductService,
    private router: Router
  ) {
    // Build the reactive form with all required validators
    this.form = this.fb.group({
      clientName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      productId:  ['', [Validators.required]],
      zoneCode:   ['', [Validators.required]],
      clientAge:  ['', [Validators.required, Validators.min(18), Validators.max(99)]]
    });
  }

  ngOnInit(): void {
    // Fetch the list of available insurance products from the backend
    // and populate the product dropdown in the form
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
      },
      error: (err) => {
        // Show error in the UI if products can't be loaded
        this.errorMessage = err.message || 'Failed to load products.';
      }
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;
    this.successMessage = null;

    // Stop here and highlight invalid fields if the form is not valid
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    // Build the QuoteRequest payload matching the backend DTO
    // Note: form values are strings by default, so we cast explicitly
    const request = {
      clientName: this.form.value.clientName as string,
      clientAge:  Number(this.form.value.clientAge),
      productId:  Number(this.form.value.productId),
      zoneCode:   this.form.value.zoneCode as string
    };

    this.loading = true;

    this.quoteService.createQuote(request).subscribe({
      next: (quote) => {
        this.successMessage = `Quote #${quote.quoteId} created successfully!`;
        this.loading = false;
        // Redirect to the detail page of the newly created quote
        this.router.navigate(['/quotes', quote.quoteId]);
      },
      error: (err) => {
        // Display the error returned by the backend (e.g. unknown zone, invalid product)
        this.errorMessage = err.message || 'Failed to create quote.';
        this.loading = false;
      }
    });
  }

  /**
   * Returns true if the given field has the specified error and has been interacted with.
   */
  hasError(fieldName: string, errorType: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.hasError(errorType) && (field.dirty || field.touched || this.submitted));
  }

  /**
   * Returns true if the field is invalid and has been interacted with.
   */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched || this.submitted));
  }

  /**
   * Returns a human-readable error message for the first error on a field.
   */
  getErrorMessage(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.errors) return '';

    if (field.hasError('required'))  return `This field is required`;
    if (field.hasError('minlength')) return `Minimum ${field.errors['minlength'].requiredLength} characters`;
    if (field.hasError('min'))       return `Minimum value is ${field.errors['min'].min}`;
    if (field.hasError('max'))       return `Maximum value is ${field.errors['max'].max}`;

    return 'Invalid input';
  }
}