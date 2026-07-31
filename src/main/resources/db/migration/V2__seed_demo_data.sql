-- Demo accounts use BCrypt hashes. Production deployments should provision
-- credentials through a dedicated identity-management flow.
INSERT INTO library_users (username, password_hash, display_name, role, enabled)
VALUES
    (
        'owner',
        '{bcrypt}$2a$10$0y5GnWYelrIcPay7fQ1pgeS7ooLBLONQWdeKV0l8dbc625nQPm/i2',
        'Library Owner',
        'OWNER',
        TRUE
    ),
    (
        'client',
        '{bcrypt}$2a$10$GnBtq4p98RvEdRIVaFEOvOWHeHGJ3YrKxNJT3O.e8bvUdOOVmZNmO',
        'Demo Client',
        'CLIENT',
        TRUE
    );

INSERT INTO books (
    isbn,
    title,
    author,
    description,
    total_copies,
    available_copies,
    active,
    version,
    created_at,
    updated_at
)
VALUES
    (
        '9780132350884',
        'Clean Code',
        'Robert C. Martin',
        'A handbook of agile software craftsmanship.',
        2,
        2,
        TRUE,
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '9780134757599',
        'Refactoring',
        'Martin Fowler',
        'Improving the design of existing code.',
        1,
        1,
        TRUE,
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '9780321125217',
        'Domain-Driven Design',
        'Eric Evans',
        'Tackling complexity in the heart of software.',
        1,
        1,
        TRUE,
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
