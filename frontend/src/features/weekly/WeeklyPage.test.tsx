import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { WeeklyReport } from '../../api/types'
import { renderAt } from '../../test/render'
import { WeeklyPage } from './WeeklyPage'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, api: vi.fn() }
})
import { api } from '../../api/client'

const mockedApi = vi.mocked(api)

const TEXT = `2026-11-02 ~ 2026-11-06 개발1팀

[해야 할 일]
- VOC 홈페이지

[실제 달성한 일]
월(11/2)
- VOC 홈페이지 (1.00MD)
  회원가입
화(11/3)
- VOC 홈페이지 (0.50MD)
수(11/4)
- 없음
목(11/5) 오후 반차
금(11/6)
- 없음

[차주 업무]
- VOC 홈페이지`

const REPORT: WeeklyReport = {
  title: '2026-11-02 ~ 2026-11-06 개발1팀',
  todo: [{ itemId: 1, title: 'VOC 홈페이지', tag: null }],
  achieved: [
    {
      itemId: 1,
      title: 'VOC 홈페이지',
      days: ['MONDAY', 'TUESDAY'],
      md: 1.5,
      completed: false,
      memos: [{ day: 'MONDAY', memo: '회원가입' }],
    },
  ],
  leaves: [{ date: '2026-11-05', type: 'HALF_PM' }],
  byDay: [
    { date: '2026-11-02', leave: null, entries: [{ itemId: 1, title: 'VOC 홈페이지', md: 1, memo: '회원가입', completed: false }] },
    { date: '2026-11-03', leave: null, entries: [{ itemId: 1, title: 'VOC 홈페이지', md: 0.5, memo: null, completed: false }] },
    { date: '2026-11-04', leave: null, entries: [] },
    { date: '2026-11-05', leave: 'HALF_PM', entries: [] },
    { date: '2026-11-06', leave: null, entries: [] },
  ],
  next: [{ itemId: 1, title: 'VOC 홈페이지', tag: null }],
  text: TEXT,
}

describe('WeeklyPage', () => {
  beforeEach(() => {
    mockedApi.mockReset()
    mockedApi.mockResolvedValue(REPORT)
  })

  it('showsTheThreeSections', async () => {
    renderAt('/weekly/2026-11-02', '/weekly/:monday?', <WeeklyPage />)

    expect(await screen.findByRole('heading', { name: '해야 할 일' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '실제 달성한 일' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '차주 업무' })).toBeInTheDocument()
    for (const dow of ['월', '화', '수', '목', '금']) expect(screen.getByText(dow)).toBeInTheDocument()
    expect(screen.getByText('11/2')).toBeInTheDocument()
    expect(screen.getByText('1.00MD')).toBeInTheDocument()
    expect(screen.getByText('0.50MD')).toBeInTheDocument()
    expect(screen.getByText('회원가입')).toBeInTheDocument()
    expect(screen.getByText('오후 반차')).toBeInTheDocument()
  })

  it('copyAll_writesServerText', async () => {
    const user = userEvent.setup()
    renderAt('/weekly/2026-11-02', '/weekly/:monday?', <WeeklyPage />)

    await user.click(await screen.findByRole('button', { name: '전체 복사' }))

    expect(await navigator.clipboard.readText()).toBe(TEXT)
  })

  it('copySection_writesOnlyThatSectionBody', async () => {
    const user = userEvent.setup()
    renderAt('/weekly/2026-11-02', '/weekly/:monday?', <WeeklyPage />)

    await user.click(await screen.findByRole('button', { name: '차주 업무 복사' }))

    expect(await navigator.clipboard.readText()).toBe('- VOC 홈페이지')
  })

  it('nonMondayParam_redirectsToMonday', async () => {
    renderAt('/weekly/2026-11-04', '/weekly/:monday?', <WeeklyPage />)

    await waitFor(() => {
      expect(mockedApi).toHaveBeenCalledWith('/api/reports/weekly?monday=2026-11-02')
    })
    expect(mockedApi).not.toHaveBeenCalledWith('/api/reports/weekly?monday=2026-11-04')
  })
})
