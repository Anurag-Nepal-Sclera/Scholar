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

-- Insert professor keywords
INSERT INTO professor_keyword (id, professor_id, keyword, normalized_keyword, weight, source) VALUES
-- John Doe (MIT - ML/NLP)
('770e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', 'Machine Learning', 'machine learning', 1.0000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440002', '660e8400-e29b-41d4-a716-446655440001', 'NLP', 'nlp', 0.9500, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001', 'Deep Learning', 'deep learning', 0.9500, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440004', '660e8400-e29b-41d4-a716-446655440001', 'Neural Networks', 'neural networks', 0.9000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440005', '660e8400-e29b-41d4-a716-446655440001', 'Transformers', 'transformers', 0.8500, 'PUBLICATION'),

-- Jane Smith (MIT - Robotics)
('770e8400-e29b-41d4-a716-446655440006', '660e8400-e29b-41d4-a716-446655440002', 'Robotics', 'robotics', 1.0000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440007', '660e8400-e29b-41d4-a716-446655440002', 'Computer Vision', 'computer vision', 0.9500, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440008', '660e8400-e29b-41d4-a716-446655440002', 'Autonomous Systems', 'autonomous systems', 0.9000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440009', '660e8400-e29b-41d4-a716-446655440002', 'SLAM', 'slam', 0.8500, 'PUBLICATION'),

-- Robert Johnson (Stanford - AI/RL)
('770e8400-e29b-41d4-a716-446655440010', '660e8400-e29b-41d4-a716-446655440003', 'Artificial Intelligence', 'artificial intelligence', 1.0000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440011', '660e8400-e29b-41d4-a716-446655440003', 'Reinforcement Learning', 'reinforcement learning', 0.9500, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440012', '660e8400-e29b-41d4-a716-446655440003', 'Neural Networks', 'neural networks', 0.9000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440013', '660e8400-e29b-41d4-a716-446655440003', 'Deep Learning', 'deep learning', 0.9000, 'PUBLICATION'),

-- Emily Brown (Cambridge - Math/Stats)
('770e8400-e29b-41d4-a716-446655440014', '660e8400-e29b-41d4-a716-446655440004', 'Optimization', 'optimization', 1.0000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440015', '660e8400-e29b-41d4-a716-446655440004', 'Statistical Learning', 'statistical learning', 0.9500, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440016', '660e8400-e29b-41d4-a716-446655440004', 'Data Science', 'data science', 0.9000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440017', '660e8400-e29b-41d4-a716-446655440004', 'Machine Learning', 'machine learning', 0.8500, 'PUBLICATION'),

-- Michael Wilson (ETH - Distributed Systems)
('770e8400-e29b-41d4-a716-446655440018', '660e8400-e29b-41d4-a716-446655440005', 'Distributed Systems', 'distributed systems', 1.0000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440019', '660e8400-e29b-41d4-a716-446655440005', 'Cloud Computing', 'cloud computing', 0.9500, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440020', '660e8400-e29b-41d4-a716-446655440005', 'Big Data', 'big data', 0.9000, 'RESEARCH_AREA'),

-- Sarah Davis (Toronto - Bioinformatics)
('770e8400-e29b-41d4-a716-446655440021', '660e8400-e29b-41d4-a716-446655440006', 'Bioinformatics', 'bioinformatics', 1.0000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440022', '660e8400-e29b-41d4-a716-446655440006', 'Computational Biology', 'computational biology', 0.9500, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440023', '660e8400-e29b-41d4-a716-446655440006', 'Genomics', 'genomics', 0.9000, 'RESEARCH_AREA'),
('770e8400-e29b-41d4-a716-446655440024', '660e8400-e29b-41d4-a716-446655440006', 'Machine Learning', 'machine learning', 0.8000, 'PUBLICATION');

-- Insert sample tenant
INSERT INTO tenant (id, name, email, status) VALUES
('880e8400-e29b-41d4-a716-446655440001', 'Sample University', 'admin@sampleuniversity.edu', 'ACTIVE');

-- Insert sample user profile
INSERT INTO user_profile (id, tenant_id, email, first_name, last_name, phone, status) VALUES
('990e8400-e29b-41d4-a716-446655440001', '880e8400-e29b-41d4-a716-446655440001', 'student@sampleuniversity.edu', 'Alice', 'Student', '+1234567890', 'ACTIVE');

-- Note: CV data would be inserted through the upload API
-- SMTP account would be configured through the SMTP API with encrypted passwords
