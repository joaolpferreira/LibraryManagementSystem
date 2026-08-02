package com.example.library.recommendation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.book.BookResponse;
import com.example.library.loan.LoanRepository;
import com.example.library.metadata.BookMetadata;
import com.example.library.metadata.BookMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private static final int AUTHOR_WEIGHT = 6;
    private static final int SUBJECT_WEIGHT = 3;
    private static final int MAX_SUBJECT_SCORE = 9;
    private static final int MAX_POPULARITY_SCORE = 5;
    private static final int MAX_RESULTS = 20;

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final BookMetadataRepository metadataRepository;

    public RecommendationService(
            BookRepository bookRepository,
            LoanRepository loanRepository,
            BookMetadataRepository metadataRepository
    ) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.metadataRepository = metadataRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommend(String username, int limit) {
        if (limit < 1 || limit > MAX_RESULTS) {
            throw new IllegalArgumentException("Recommendation limit must be between 1 and 20");
        }
        List<Book> history = loanRepository.findBorrowedBooksForRecommendations(username);
        Set<Long> previouslyBorrowed = new HashSet<>();
        Set<String> preferredAuthors = new HashSet<>();
        for (Book book : history) {
            previouslyBorrowed.add(book.getId());
            preferredAuthors.add(normalize(book.getAuthor()));
        }

        List<Book> candidates = bookRepository.findByActiveTrueOrderByTitleAsc().stream()
                .filter(book -> !previouslyBorrowed.contains(book.getId()))
                .toList();
        Map<Long, Long> popularity = new HashMap<>();
        loanRepository.countLoansByBook().forEach(count ->
                popularity.put(count.getBookId(), count.getLoanCount()));

        Set<String> preferredSubjects = preferredSubjects(previouslyBorrowed);
        Map<Long, Set<String>> candidateSubjects = subjectsByBook(
                candidates.stream().map(Book::getId).toList()
        );

        return candidates.stream()
                .map(book -> score(
                        book,
                        preferredAuthors,
                        preferredSubjects,
                        candidateSubjects.getOrDefault(book.getId(), Set.of()),
                        popularity.getOrDefault(book.getId(), 0L)
                ))
                .sorted((left, right) -> {
                    int byScore = Integer.compare(right.score(), left.score());
                    return byScore != 0
                            ? byScore
                            : left.book().title().compareToIgnoreCase(right.book().title());
                })
                .limit(limit)
                .toList();
    }

    private Set<String> preferredSubjects(Set<Long> previouslyBorrowed) {
        if (previouslyBorrowed.isEmpty()) {
            return Set.of();
        }
        Set<String> subjects = new HashSet<>();
        metadataRepository.findByBookIdIn(previouslyBorrowed).forEach(metadata ->
                metadata.getSubjects().stream().map(RecommendationService::normalize)
                        .forEach(subjects::add));
        return subjects;
    }

    private Map<Long, Set<String>> subjectsByBook(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> result = new HashMap<>();
        for (BookMetadata metadata : metadataRepository.findByBookIdIn(bookIds)) {
            Set<String> subjects = new HashSet<>();
            metadata.getSubjects().stream().map(RecommendationService::normalize)
                    .forEach(subjects::add);
            result.put(metadata.getBookId(), subjects);
        }
        return result;
    }

    private RecommendationResponse score(
            Book book,
            Set<String> preferredAuthors,
            Set<String> preferredSubjects,
            Set<String> candidateSubjects,
            long loanCount
    ) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (book.getAvailableCopies() > 0) {
            score++;
            reasons.add("Available now");
        }
        if (preferredAuthors.contains(normalize(book.getAuthor()))) {
            score += AUTHOR_WEIGHT;
            reasons.add("Matches an author from your borrowing history");
        }
        long overlappingSubjects = candidateSubjects.stream()
                .filter(preferredSubjects::contains)
                .count();
        if (overlappingSubjects > 0) {
            score += Math.min((int) overlappingSubjects * SUBJECT_WEIGHT, MAX_SUBJECT_SCORE);
            reasons.add("Shares subjects with books you borrowed");
        }
        if (loanCount > 0) {
            score += (int) Math.min(loanCount, MAX_POPULARITY_SCORE);
            reasons.add("Popular with library clients");
        }
        if (reasons.isEmpty()) {
            reasons.add("Explore something new from the catalog");
        }
        return new RecommendationResponse(BookResponse.from(book), score, reasons);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
