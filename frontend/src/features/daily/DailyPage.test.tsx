import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { DayView } from '../../api/types'
import { renderAt } from '../../test/render'
import { DailyPage } from './DailyPage'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, api: vi.fn() }
})
import { api } from '../../api/client'

const mockedApi = vi.mocked(api)

function dayFixture(overrides: Partial<DayView> = {}): DayView {
  return {
    date: '2026-11-02',
    dayType: 'WORK',
    dayTypeLabel: '근무',
    workMd: 1,
    memo: null,
    entries: [
      { itemId: 1, title: 'VOC', tag: null, status: 'IN_PROGRESS', recurring: false, weight: 0, md: 0, memo: null, completedBefore: false },
      { itemId: 2, title: '회의', tag: null, status: 'IN_PROGRESS', recurring: true, weight: 0, md: 0, memo: null, completedBefore: false },
    ],
    waitingItems: [],
    ...overrides,
  }
}

function mockServer(day: DayView) {
  mockedApi.mockImplementation(async (path: string) => {
    if (path.startsWith('/api/days?')) return []
    if (path === `/api/days/${day.date}`) return day
    throw new Error(`unexpected ${path}`)
  })
}

function renderDaily() {
  return renderAt('/daily/2026-11-02', '/daily/:date?', <DailyPage />)
}

describe('DailyPage', () => {
  beforeEach(() => {
    mockedApi.mockReset()
  })

  it('selectingWeights_updatesPreview', async () => {
    mockServer(dayFixture())
    const user = userEvent.setup()
    renderDaily()

    await screen.findByText('VOC')
    await user.click(within(screen.getByRole('group', { name: 'VOC 비중' })).getByRole('button', { name: '2' }))
    await user.click(within(screen.getByRole('group', { name: '회의 비중' })).getByRole('button', { name: '1' }))

    const panel = screen.getByRole('region', { name: '오늘 투입 공수' })
    expect(within(panel).getByText('0.67MD · 5.4h')).toBeInTheDocument()
    expect(within(panel).getByText('0.33MD · 2.6h')).toBeInTheDocument()
    expect(within(panel).getByText('1.00MD (8.0h)')).toBeInTheDocument()
  })

  it('save_sendsWeightsAndMemos', async () => {
    const day = dayFixture()
    mockServer(day)
    const user = userEvent.setup()
    renderDaily()

    await screen.findByText('VOC')
    await user.click(within(screen.getByRole('group', { name: 'VOC 비중' })).getByRole('button', { name: '2' }))
    fireEvent.change(within(screen.getByTestId('entry-1')).getByPlaceholderText('메모 추가…'), {
      target: { value: '회원가입' },
    })
    fireEvent.change(screen.getByPlaceholderText('하루 메모 (선택)'), { target: { value: '정리 필요' } })
    await user.click(screen.getByRole('button', { name: '11월 2일 체크인 저장' }))

    await waitFor(() => {
      const putCall = mockedApi.mock.calls.find(([path, init]) => path === '/api/days/2026-11-02' && init?.method === 'PUT')
      expect(putCall).toBeDefined()
      expect(JSON.parse(putCall![1]!.body as string)).toEqual({
        memo: '정리 필요',
        entries: [
          { itemId: 1, weight: 2, memo: '회원가입' },
          { itemId: 2, weight: 0, memo: '' },
        ],
      })
    })
  })

  it('annualDay_disablesSegments', async () => {
    mockServer(dayFixture({ dayType: 'ANNUAL', dayTypeLabel: '연차', workMd: 0 }))
    renderDaily()

    await screen.findByText('VOC')

    const buttons = within(screen.getByRole('group', { name: 'VOC 비중' })).getAllByRole('button')
    expect(buttons).toHaveLength(4)
    buttons.forEach((button) => expect(button).toBeDisabled())
    expect(screen.getByText(/업무 비중을 입력할 수 없어요/)).toBeInTheDocument()
  })
})
