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
- VOC 홈페이지 (월·화 / 1.50MD)
  · 월: 회원가입
- 11/5(목) 오후 반차

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
    expect(screen.getByText('월·화 / 1.50MD')).toBeInTheDocument()
    expect(screen.getByText('회원가입')).toBeInTheDocument()
    expect(screen.getByText('11/5(목) 오후 반차')).toBeInTheDocument()
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
