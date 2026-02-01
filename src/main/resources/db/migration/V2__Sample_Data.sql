-- Sample Data for Development and Testing
-- This script populates the database with realistic test data

-- Insert sample universities
INSERT INTO university (id, name, country, website, rank_global, status) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'Massachusetts Institute of Technology', 'USA', 'https://mit.edu', 1, 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440002', 'Stanford University', 'USA', 'https://stanford.edu', 2, 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440003', 'University of Cambridge', 'UK', 'https://cam.ac.uk', 3, 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440004', 'ETH Zurich', 'Switzerland', 'https://ethz.ch', 8, 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440005', 'University of Toronto', 'Canada', 'https://utoronto.ca', 18, 'ACTIVE');

-- Insert sample professors
INSERT INTO professor (id, university_id, email, first_name, last_name, department, research_area, status) VALUES
('660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'john.doe@mit.edu', 'John', 'Doe', 'Computer Science', 'Machine Learning, Natural Language Processing, Deep Learning', 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'jane.smith@mit.edu', 'Jane', 'Smith', 'Electrical Engineering', 'Robotics, Computer Vision, Autonomous Systems', 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440002', 'robert.johnson@stanford.edu', 'Robert', 'Johnson', 'Computer Science', 'Artificial Intelligence, Reinforcement Learning, Neural Networks', 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440003', 'emily.brown@cam.ac.uk', 'Emily', 'Brown', 'Mathematics', 'Optimization, Statistical Learning, Data Science', 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440004', 'michael.wilson@ethz.ch', 'Michael', 'Wilson', 'Computer Science', 'Distributed Systems, Cloud Computing, Big Data', 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440005', 'sarah.davis@utoronto.ca', 'Sarah', 'Davis', 'Computer Science', 'Bioinformatics, Computational Biology, Genomics', 'ACTIVE');

-- Insert sample tenant
INSERT INTO tenant (id, name, email, status) VALUES
('880e8400-e29b-41d4-a716-446655440001', 'Sample University', 'admin@sampleuniversity.edu', 'ACTIVE');

-- Insert sample user profile
INSERT INTO user_profile (id, tenant_id, email, first_name, last_name, phone, status) VALUES
('990e8400-e29b-41d4-a716-446655440001', '880e8400-e29b-41d4-a716-446655440001', 'student@sampleuniversity.edu', 'Alice', 'Student', '+1234567890', 'ACTIVE');

-- Note: CV data would be inserted through the upload API
-- SMTP account would be configured through the SMTP API with encrypted passwords
