import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { EffortReport, MonthlySummary, WorkItem } from '../../api/types'
import { renderAt } from '../../test/render'
import { MonthlyPage } from './MonthlyPage'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, api: vi.fn() }
})
import { api } from '../../api/client'

const mockedApi = vi.mocked(api)

const ITEMS: WorkItem[] = [
  { id: 1, title: '위시드콘 이슈처리', tag: '위시드콘', status: 'IN_PROGRESS', recurring: false, showInPlan: true, sortOrder: 1, startedAt: '2026-04-01', completedAt: null, totalMd: 3 },
  { id: 2, title: '끝난 업무', tag: null, status: 'DONE', recurring: false, showInPlan: true, sortOrder: 2, startedAt: '2026-04-01', completedAt: '2026-06-30', totalMd: 2 },
]

const EFFORT: EffortReport = {
  label: '위시드콘',
  from: '2026-01',
  to: '2026-06',
  months: [
    { month: '2026-05', md: 0.5 },
    { month: '2026-06', md: 1 },
  ],
  total: 1.5,
  text: '위시드콘 투입 공수 (2026-01 ~ 2026-06)\n5월 0.50MD\n6월 1.00MD\n합계 1.50MD',
}

const MONTHLY: MonthlySummary = {
  month: '2026-11',
  rows: [
    { tag: '위시드', title: 'VOC', md: 2.17, ratioPercent: 86.8 },
    { tag: null, title: '회의', md: 0.33, ratioPercent: 13.2 },
  ],
  workDays: 3,
  leaveCounts: { ANNUAL: 1, HALF_AM: 1, HALF_PM: 0, HOLIDAY: 0 },
  totalMd: 2.5,
  tsv: '태그\t업무\tMD\t비율(%)\n위시드\tVOC\t2.17\t86.8\n\t회의\t0.33\t13.2',
}

function mockServer() {
  mockedApi.mockImplementation(async (path: string) => {
    if (path === '/api/work-items') return ITEMS
    if (path === '/api/work-items/tags') return ['위시드콘']
    if (path.startsWith('/api/reports/effort?')) return EFFORT
    if (path.startsWith('/api/reports/monthly?')) return MONTHLY
    throw new Error(`unexpected ${path}`)
  })
}

describe('MonthlyPage', () => {
  beforeEach(() => {
    mockedApi.mockReset()
    mockServer()
  })

  it('effortQuery_rendersMonthsAndCopiesText', async () => {
    const user = userEvent.setup()
    renderAt('/monthly', '/monthly', <MonthlyPage />)

    await screen.findByRole('option', { name: '위시드콘' })
    await user.selectOptions(screen.getByLabelText('대상'), 'tag:위시드콘')

    const result = await screen.findByRole('region', { name: '공수 조회 결과' })
    expect(within(result).getByText('5월')).toBeInTheDocument()
    expect(within(result).getByText('0.50MD')).toBeInTheDocument()
    expect(within(result).getByText('1.50MD')).toBeInTheDocument()
    const call = mockedApi.mock.calls.find(([p]) => p.startsWith('/api/reports/effort?'))
    expect(call?.[0]).toContain(`tag=${encodeURIComponent('위시드콘')}`)

    await user.click(screen.getByRole('button', { name: '결과 복사' }))
    expect(await navigator.clipboard.readText()).toBe(EFFORT.text)
  })

  it('effortTargets_listDoneItemsWithMarker', async () => {
    renderAt('/monthly', '/monthly', <MonthlyPage />)

    expect(await screen.findByRole('option', { name: '끝난 업무 (완료)' })).toBeInTheDocument()
  })

  it('monthlyCopy_writesTsv', async () => {
    const user = userEvent.setup()
    renderAt('/monthly', '/monthly', <MonthlyPage />)

    await user.click(screen.getByRole('button', { name: '월간 요약' }))

    expect(await screen.findByText('86.8%')).toBeInTheDocument()
    expect(screen.getByText('근무일 3일')).toBeInTheDocument()
    expect(screen.getByText('연차 1')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '표 복사' }))
    expect(await navigator.clipboard.readText()).toBe(MONTHLY.tsv)
  })
})
