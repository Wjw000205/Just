CREATE UNIQUE INDEX uk_sys_user_phone
    ON sys_user(phone)
    WHERE deleted = 0 AND phone IS NOT NULL AND phone <> '';
