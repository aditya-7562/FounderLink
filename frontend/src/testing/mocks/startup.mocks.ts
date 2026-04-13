import { StartupResponse } from '../../app/models';

export const MOCK_STARTUP: StartupResponse = {
  id: 1,
  name: 'Test Startup',
  description: 'A test startup description',
  industry: 'Tech',
  stage: 'EARLY_TRACTION',
  fundingGoal: 100000,
  problemStatement: 'The problem is hard',
  solution: 'The solution is easy',
  founderId: 10,
  createdAt: '2023-01-01T00:00:00Z',
  moderationStatus: 'APPROVED',
  moderationReason: null
};

export const MOCK_STARTUP_LIST: StartupResponse[] = [
  MOCK_STARTUP,
  { ...MOCK_STARTUP, id: 2, name: 'Second Startup' }
];
