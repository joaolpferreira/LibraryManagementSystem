package com.example.library.loan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookReturnedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookReturnedEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookReturned(BookReturnedEvent event) {
        LOGGER.info(
                "Book returned: loanId={}, bookId={}, username={}, late={}",
                event.loanId(),
                event.bookId(),
                event.username(),
                event.late()
        );
    }
}

