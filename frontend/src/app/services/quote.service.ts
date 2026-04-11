import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { QuoteRequest, QuoteResponse } from '../models/quote.model';

/**
 * Service for all quote-related API communication with the backend pricing engine
 */
@Injectable({
  providedIn: 'root'
})
export class QuoteService {
  private readonly apiUrl = environment.apiUrl;
  private readonly endpoint = '/quotes';

  constructor(private http: HttpClient) {}

  /**
   * POST /api/quotes
   */
  createQuote(request: QuoteRequest): Observable<QuoteResponse> {
    return this.http
      .post<QuoteResponse>(`${this.apiUrl}${this.endpoint}`, request)
      .pipe(catchError(this.handleError));
  }

  /**
   * GET /api/quotes/:id
   */
  getQuote(id: number): Observable<QuoteResponse> {
    return this.http
      .get<QuoteResponse>(`${this.apiUrl}${this.endpoint}/${id}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Fetch all quotes, with optional filtering by product and minimum price.
   * GET /api/quotes?productId=X&minPrice=Y
   */
  getQuotes(filters?: { productId?: number; minPrice?: number }): Observable<QuoteResponse[]> {
    // Build query params only for filter values that were actually provided
    let params = new HttpParams();
    if (filters?.productId !== undefined) {
      params = params.set('productId', filters.productId);
    }
    if (filters?.minPrice !== undefined) {
      params = params.set('minPrice', filters.minPrice);
    }

    return this.http
      .get<QuoteResponse[]>(`${this.apiUrl}${this.endpoint}`, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Maps HTTP errors to user-friendly messages.
   * Prefers the message from the backend response body when available.
   */
  private handleError(error: any): Observable<never> {
    console.error('Quote service error:', error);

    // Use the backend error message if provided, otherwise fall back to defaults
    const message =
      error.error?.message ||
      (error.status === 404 ? 'Quote not found.' :
       error.status === 400 ? 'Invalid request data.' :
       'Failed to process quote.');

    return throwError(() => new Error(message));
  }
}