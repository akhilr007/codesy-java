ALTER TABLE problem_versions
ADD COLUMN IF NOT EXISTS java_starter_code TEXT,
ADD COLUMN IF NOT EXISTS java_execution_template TEXT,
ADD COLUMN IF NOT EXISTS python_starter_code TEXT,
ADD COLUMN IF NOT EXISTS python_execution_template TEXT,
ADD COLUMN IF NOT EXISTS cpp_starter_code TEXT,
ADD COLUMN IF NOT EXISTS cpp_execution_template TEXT;