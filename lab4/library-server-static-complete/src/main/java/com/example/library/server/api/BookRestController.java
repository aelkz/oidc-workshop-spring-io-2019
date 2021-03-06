package com.example.library.server.api;

import com.example.library.server.api.resource.BookListResource;
import com.example.library.server.api.resource.BookResource;
import com.example.library.server.business.BookService;
import com.example.library.server.dataaccess.Book;
import com.example.library.server.security.LibraryUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Collectors;

/** REST api for books. */
@RestController
@RequestMapping("/books")
@Validated
public class BookRestController {

  private final BookService bookService;

  @Autowired
  public BookRestController(BookService bookService) {
    this.bookService = bookService;
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping
  public BookListResource getAllBooks() {
    return new BookListResource(bookService.findAll().stream().map(BookResource::new).collect(Collectors.toList()));
  }

  @GetMapping("/{bookId}")
  public ResponseEntity<BookResource> getBookById(@PathVariable("bookId") UUID bookIdentifier) {
    return bookService
        .findWithDetailsByIdentifier(bookIdentifier)
        .map(BookResource::new)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/{bookId}/borrow")
  public ResponseEntity<BookResource> borrowBookById(
      @PathVariable("bookId") UUID bookId, @AuthenticationPrincipal LibraryUser libraryUser) {
    return bookService
        .findByIdentifier(bookId)
        .map(
            b -> {
              bookService.borrowById(bookId, libraryUser.getIdentifier());
              return bookService
                  .findWithDetailsByIdentifier(b.getIdentifier())
                  .map(bb -> ResponseEntity.ok(new BookResource(bb)))
                  .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/{bookId}/return")
  public ResponseEntity<BookResource> returnBookById(
      @PathVariable("bookId") UUID bookId, @AuthenticationPrincipal LibraryUser libraryUser) {
    return bookService
        .findByIdentifier(bookId)
        .map(
            b -> {
              bookService.returnById(bookId, libraryUser.getIdentifier());
              return bookService
                  .findWithDetailsByIdentifier(b.getIdentifier())
                  .map(bb -> ResponseEntity.ok(new BookResource(bb)))
                  .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<BookResource> createBook(@RequestBody BookResource bookResource) {

    Book book =
        new Book(
            bookResource.getIdentifier(),
            bookResource.getIsbn(),
            bookResource.getTitle(),
            bookResource.getDescription(),
            bookResource.getAuthors(),
            bookResource.isBorrowed(),
            null);

    UUID identifier = bookService.create(book);

    return bookService
        .findWithDetailsByIdentifier(identifier)
        .map(b -> new BookResource(b))
        .map(
            b -> {
              URI location =
                  ServletUriComponentsBuilder.fromCurrentContextPath()
                      .path("/books/{bookId}")
                      .buildAndExpand(b.getIdentifier())
                      .toUri();
              return ResponseEntity.created(location).body(b);
            })
        .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  @PutMapping("/{bookId}")
  public ResponseEntity<BookResource> updateBook(
      @PathVariable("bookId") UUID bookId, @RequestBody BookResource bookResource) {

    return bookService
        .findByIdentifier(bookId)
        .map(
            b -> {
              b.setAuthors(bookResource.getAuthors());
              b.setDescription(bookResource.getDescription());
              b.setIsbn(bookResource.getIsbn());
              b.setTitle(bookResource.getTitle());
              UUID identifier = bookService.update(b);
              return bookService
                  .findWithDetailsByIdentifier(identifier)
                  .map(ub -> ResponseEntity.ok(new BookResource(ub)))
                  .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{bookId}")
  public void deleteBook(@PathVariable("bookId") UUID bookId) {
    bookService.deleteByIdentifier(bookId);
  }
}
