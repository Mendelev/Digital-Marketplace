-- Manual cleanup script for existing orphaned users
-- Run this on user_db before deployment to clean up any orphaned users

-- Step 1: Identify orphaned users (users in user_db without credentials in auth_db)
-- Note: This query requires connecting to both databases
-- For PostgreSQL, you can use dblink or run queries separately

-- Query to run on user_db to get all user IDs:
SELECT user_id, email, name, created_at 
FROM users 
ORDER BY created_at;

-- Query to run on auth_db to get all credential user IDs:
SELECT user_id, email, created_at 
FROM credentials 
ORDER BY created_at;

-- Step 2: Manual verification
-- Compare the results and identify user_ids that exist in users table but not in credentials table

-- Step 3: Delete orphaned users (example - replace with actual orphaned user_ids)
-- WARNING: This will permanently delete user data. Verify orphaned users before running.

-- Delete from dependent tables first
DELETE FROM user_preferences WHERE user_id IN (
    -- List orphaned user_ids here
    -- '00000000-0000-0000-0000-000000000000'
);

DELETE FROM addresses WHERE user_id IN (
    -- List orphaned user_ids here
    -- '00000000-0000-0000-0000-000000000000'
);

-- Delete from users table
DELETE FROM users WHERE user_id IN (
    -- List orphaned user_ids here
    -- '00000000-0000-0000-0000-000000000000'
);

-- Step 4: Verify cleanup
SELECT COUNT(*) as orphaned_users_count 
FROM users u 
WHERE NOT EXISTS (
    SELECT 1 FROM credentials c WHERE c.user_id = u.user_id
);
-- Should return 0 after successful cleanup
