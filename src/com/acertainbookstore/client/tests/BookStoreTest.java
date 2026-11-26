package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook; // to test rateBooks
import com.acertainbookstore.business.BookRating; // to test rateBooks
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test that buying zero books does not change stock
	 * @throws BookStore
	 */
	@Test
	public void testBuyZeroNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN,0));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}
		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());

	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books returned to clients do not expose stock information,
	 * such as the number of copies and sales misses.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testClientViewHidesStockInfo() throws BookStoreException {
		Set<Integer> isbnSet = new HashSet<Integer>();
		isbnSet.add(TEST_ISBN);

		List<Book> booksFromGetBooks = client.getBooks(isbnSet);
		List<Book> booksFromTopRated = client.getTopRatedBooks(1);

		assertEquals(1, booksFromGetBooks.size());
		assertEquals(1, booksFromTopRated.size());
		assertFalse(booksFromGetBooks.get(0) instanceof StockBook);
		assertFalse(booksFromTopRated.get(0) instanceof StockBook);
	}

//	Tests on getBooksInDemand:
//	Test inDemand is empty before any miss
	@Test
	public void testNoneInDemandBeforeAnyMiss() throws BookStoreException {
		List<StockBook> booksInDemand = storeManager.getBooksInDemand();

		assertEquals(0, booksInDemand.size());
	}

//	Test buying to stock 0 will not trigger flagging
	@Test
	public void testBuyingLastStockWillNotPutBookInDemand() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Buying the last copies in stock
		client.buyBooks(booksToBuy);

		// The book is sold out
		assertEquals(0, storeManager.getBooks().get(0).getNumCopies());

		// but not in demand
		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		assertEquals(0, booksInDemand.size());
	}

//	Test buying when stock 0 will trigger flagging
	@Test
	public void testBuyingWhenNoStockPutsBookInDemand() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Buying the last copies in stock
		client.buyBooks(booksToBuy);

		// Asking for a copy when there is no stock
		booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		assertEquals(1, booksInDemand.size());

		StockBook bookInDemand = booksInDemand.get(0);
		assertTrue(bookInDemand.getISBN() == TEST_ISBN && bookInDemand.getNumSaleMisses() == 1);
	}

//	Test is book in demand
	@Test
	public void testCertainBookIsInDemand() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		// Asking for more than is in stock
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
        assertEquals(1, booksInDemand.size());

		StockBook bookInDemand = booksInDemand.get(0);
		assertTrue(bookInDemand.getISBN() == TEST_ISBN && bookInDemand.getNumSaleMisses() == 1);
	}

//	Test is list of books in demand
	@Test
	public void testMultipleBooksInDemand() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		addBooks(TEST_ISBN + 1, NUM_COPIES);
		List<StockBook> expectedBooks = storeManager.getBooks();

		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES + 1));
		// Asking for more than is in stock
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		assertTrue(expectedBooks.containsAll(booksInDemand) && expectedBooks.size() == booksInDemand.size());

	}

//	Test buyBooks does not flag when part of order fails - all-or-nothing.
	@Test
	public void testBuyBooksEnforcesAllOrNothingSemantics() throws BookStoreException {
		addBooks(TEST_ISBN + 1, NUM_COPIES);

		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // enough copies in stock
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES + 1)); // more than is in stock
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		// Only the book without enough copies is flagged
		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		assertEquals(1, booksInDemand.size());

		StockBook bookInDemand = booksInDemand.get(0);
		assertTrue(bookInDemand.getISBN() == TEST_ISBN + 1 && bookInDemand.getNumSaleMisses() == 1);

		// Nothing is sold when the order fails
		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePostTest.size() == 2);
		for (StockBook book : booksInStorePostTest) {
			assertTrue(book.getNumCopies() == NUM_COPIES);
		}
	}

//	Test InDemand list shrinks when stock is added
	@Test
	public void testInDemandShrinksWhenBookIsRestocked() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		addBooks(TEST_ISBN + 1, NUM_COPIES);

		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES + 1));
		// Asking for more than is in stock
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		// Both books are in demand
		assertEquals(2, storeManager.getBooksInDemand().size());

		// Restocking one of the books
		Set<BookCopy> bookCopiesSet = new HashSet<BookCopy>();
		bookCopiesSet.add(new BookCopy(TEST_ISBN, NUM_COPIES));
		storeManager.addCopies(bookCopiesSet);

		// Only the book that was not restocked is still in demand
		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		assertEquals(1, booksInDemand.size());

		StockBook bookInDemand = booksInDemand.get(0);
		assertTrue(bookInDemand.getISBN() == TEST_ISBN + 1 && bookInDemand.getNumSaleMisses() == 1);
	}

//	Test miss count is correct
	@Test
	public void testMissCountIsUpdatedCorrectly() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		// Asking for one copy more than is in stock
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 3));

		// Asking for three copies more than is in stock
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		assertEquals(1, booksInDemand.size());

		// The misses of both orders are added up
		StockBook bookInDemand = booksInDemand.get(0);
		assertTrue(bookInDemand.getISBN() == TEST_ISBN && bookInDemand.getNumSaleMisses() == 4);
	}

//	Test inDemand list stays same when same buyBooks sends same flag.
	@Test
	public void testInDemandStaysInDemandAfterOrders() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		addBooks(TEST_ISBN + 1, NUM_COPIES);

		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES + 1));
		// Asking for more than is in stock
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		List<StockBook> booksInDemandBefore = storeManager.getBooksInDemand();
		assertEquals(2, booksInDemandBefore.size());

		// Sending the same order again
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ignored) {
			;
		}

		// The same books are in demand, each only once
		List<StockBook> booksInDemandAfter = storeManager.getBooksInDemand();
		assertEquals(booksInDemandBefore.size(), booksInDemandAfter.size());
		assertTrue(booksInDemandAfter.containsAll(booksInDemandBefore));
	}

	// Ratebooks:
	// valid more than one book
	@Test
	public void testRateBooksmultiple() throws BookStoreException {

		// add a book inside of of the bookstore to rate list
		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 0)); // valid

		// add a book from outside of the bookstore into the bookstore
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "Advanced Computer SUStems", "Sofus Heiberg",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);

		// add that book to the bookstorate list
		booksToRate.add(new BookRating(TEST_ISBN + 1, 5)); // valid

		client.rateBooks(booksToRate);


		// retrieve both books after
		List<StockBook> books = storeManager.getBooks();
		assertEquals(2, books.size());

		StockBook book1 = books.get(0);
		StockBook book2 = books.get(1);

		assertEquals(1, book1.getNumTimesRated());
		assertEquals(1, book2.getNumTimesRated());

		// Check that the total ratings are correct
		if (book1.getISBN() == TEST_ISBN) {
			assertEquals(0, book1.getTotalRating());
			assertEquals(5, book2.getTotalRating());
		} else {
			assertEquals(5, book1.getTotalRating());
			assertEquals(0, book2.getTotalRating());
		}
	}
	// valid ISBN
	@Test
	public void testRateBooksValidISBN() throws BookStoreException {
		StockBook addedBookBefore = storeManager.getBooks().get(0);

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 0)); // valid

		client.rateBooks(booksToRate);

		StockBook addedBookafter = storeManager.getBooks().get(0);

		assertTrue(addedBookBefore.getNumTimesRated() < addedBookafter.getNumTimesRated());
	}

	// rating skal være mellem 0-5
	@Test
	public void testRateBooksInvalidRange() throws BookStoreException {
		StockBook addedBookBefore = storeManager.getBooks().get(0);

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 6)); // invalid

		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		booksToRate.clear();

		StockBook addedBookMiddle = storeManager.getBooks().get(0);
		assertEquals(addedBookBefore.getNumTimesRated(), addedBookMiddle.getNumTimesRated());
		assertEquals(addedBookBefore.getTotalRating(), addedBookMiddle.getTotalRating());

		booksToRate.add(new BookRating(TEST_ISBN, -1)); // invalid

		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		StockBook addedBookafter = storeManager.getBooks().get(0);
		assertEquals(addedBookMiddle.getNumTimesRated(), addedBookafter.getNumTimesRated());
		assertEquals(addedBookMiddle.getTotalRating(), addedBookafter.getTotalRating());
	}

	// hvis flere så skal alle fejle hvis bare en fejler.
	// input skal være valid ISBN
	@Test
	public void testRateBooksInvalidISBN() throws BookStoreException {
		// All books in the store
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		StockBook bookInListPre = booksInStorePreTest.get(0);

		// Try and rate a book with an invalid ISBNim
		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 5)); // valid
		booksToRate.add(new BookRating(-1, 5)); // invalid

		// try catch to rate one with invalid ISBN (not added )

		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> bookInStorePost = storeManager.getBooks();
		StockBook bookInListPost = bookInStorePost.get(0);

		assertEquals(bookInListPre.getNumTimesRated(), bookInListPost.getNumTimesRated());
		assertEquals(bookInListPre.getTotalRating(), bookInListPost.getTotalRating());

	}



	// topRatedBooks:
//	@Override
//	public synchronized List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
//		throw new BookStoreException();
//	}

	//check if the top rated books are actually top rated
	@Test
	public void testGetTopRatedBooks() throws BookStoreException {

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		// book 1 has rating 1
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "whatever", "The What man",
				(float) 300, NUM_COPIES, 0, 1, 1, false));
		// book 2 has rating 5
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "hell no",
				"the hell no man", (float) 50, NUM_COPIES, 0, 1, 5, false));
		// add them to the bookstore
		storeManager.addBooks(booksToAdd);

		// the test book now should have (0+5)/2 = 2.5 as it's rating beating 1
		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 0));

		client.rateBooks(booksToRate);
		booksToRate.clear();

		booksToRate.add(new BookRating(TEST_ISBN, 5));
		client.rateBooks(booksToRate);

		//only pick out 2 books
		List<Book> bestBooks = client.getTopRatedBooks(2);

		assertEquals(2, bestBooks.size());
		assertTrue(bestBooks.get(0).getISBN() == TEST_ISBN + 2 ||
				bestBooks.get(1).getISBN() == TEST_ISBN + 2);

		assertTrue(bestBooks.get(0).getISBN() == TEST_ISBN ||
				bestBooks.get(1).getISBN() == TEST_ISBN);
	}

	//check if invalid k throws exception
	@Test
	public void testGetTopRatedBooksInvalidK() throws BookStoreException {
		try {
			client.getTopRatedBooks(-1);
			fail();
		} catch (BookStoreException ex) {
			;
		}
	}

	// check if out of bounds give correct values
	@Test
	public void testGetTopRatedBooksOutOfBounds() throws BookStoreException {

		// there is only one book to begin with so this should return empty list
		List<Book> bestBooksZero = client.getTopRatedBooks(0);

		assertEquals(0, bestBooksZero.size());

		// there is only one book so more than one book should just return that one
		List<Book> bestBooksOne = client.getTopRatedBooks(2);

		assertEquals(1, bestBooksOne.size());

	}

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
