import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Product } from '../models/product.model';

/**
 * Service for fetching available insurance products from the backend.
 */
@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly apiUrl = environment.apiUrl;
  private readonly endpoint = '/products';

  constructor(private http: HttpClient) {}

  /**
   * Fetch all available insurance products.
   * GET /api/products
   */
  getProducts(): Observable<Product[]> {
    return this.http
      .get<Product[]>(`${this.apiUrl}${this.endpoint}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Maps HTTP errors to a error message.
   */
  private handleError(error: any): Observable<never> {
    console.error('Product service error:', error);
    return throwError(() => new Error('Failed to load products'));
  }
}