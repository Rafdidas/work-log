import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { WorkItem } from '../../api/types'
import { renderAt } from '../../test/render'
import { ItemsPage } from './ItemsPage'

vi.mock('../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/client')>()
  return { ...actual, api: vi.fn() }
})
import { ApiError, api } from '../../api/client'

const mockedApi = vi.mocked(api)

function item(overrides: Partial<WorkItem>): WorkItem {
  return {
    id: 1,
    title: 'VOC 홈페이지',
    tag: '위시드',
    status: 'IN_PROGRESS',
    recurring: false,
    showInPlan: true,
    sortOrder: 1,
    startedAt: '2026-11-02',
    completedAt: null,
    totalMd: 1.5,
    ...overrides,
  }
}

const ITEMS: WorkItem[] = [
  item({ id: 1 }),
  item({ id: 2, title: '대기 업무', tag: null, status: 'WAITING', startedAt: null, totalMd: 0, sortOrder: 2 }),
  item({ id: 3, title: '끝난 업무', tag: null, status: 'DONE', completedAt: '2026-10-30', sortOrder: 3 }),
]

function mockServer() {
  mockedApi.mockImplementation(async (path: string, init?: RequestInit) => {
    if (path === '/api/work-items' && !init?.method) return ITEMS
    if (path === '/api/work-items/tags') return ['위시드']
    if (path.startsWith('/api/days/leaves')) return []
    if (path === '/api/me') return { id: 1, email: 'a@example.com', name: '테스터', teamLabel: '개발1팀' }
    return {}
  })
}

function bodyOf(path: string, method: string) {
  const call = mockedApi.mock.calls.find(([p, init]) => p === path && init?.method === method)
  return call ? JSON.parse(call[1]!.body as string) : undefined
}

describe('ItemsPage', () => {
  beforeEach(() => {
    mockedApi.mockReset()
    mockServer()
  })

  it('groupsItemsByStatus', async () => {
    renderAt('/items', '/items', <ItemsPage />)

    expect(await screen.findByTestId('item-1')).toHaveTextContent('VOC 홈페이지')
    expect(screen.getByRole('heading', { name: /진행 중/ })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /대기/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /완료 \(1\)/ })).toHaveAttribute('aria-expanded', 'false')
  })

  it('createItem_postsShowInPlan', async () => {
    const user = userEvent.setup()
    renderAt('/items', '/items', <ItemsPage />)

    await user.click(await screen.findByRole('button', { name: '새 업무' }))
    await user.type(screen.getByLabelText('업무 이름'), '새 프로젝트')
    await user.click(screen.getByLabelText('주간 보고 계획 섹션에 표시'))
    await user.click(screen.getByRole('button', { name: '항목 등록' }))

    await waitFor(() => {
      expect(bodyOf('/api/work-items', 'POST')).toEqual({
        title: '새 프로젝트',
        recurring: false,
        showInPlan: false,
        status: 'IN_PROGRESS',
      })
    })
  })

  it('deleteWithEntries_showsCompleteHint', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    mockedApi.mockImplementation(async (path: string, init?: RequestInit) => {
      if (init?.method === 'DELETE') {
        throw new ApiError(409, 'HAS_ENTRIES', '기록이 있는 항목은 삭제할 수 없습니다.')
      }
      if (path === '/api/work-items') return ITEMS
      if (path === '/api/work-items/tags') return []
      if (path.startsWith('/api/days/leaves')) return []
      return { id: 1, email: 'a@example.com', name: '테스터', teamLabel: '개발1팀' }
    })
    const user = userEvent.setup()
    renderAt('/items', '/items', <ItemsPage />)

    const row = await screen.findByTestId('item-1')
    await user.click(within(row).getByRole('button', { name: '삭제' }))

    expect(await screen.findByText('기록이 있어 삭제할 수 없어요. 완료 처리해 주세요.')).toBeInTheDocument()
  })

  it('registerLeave_putsDayType', async () => {
    const user = userEvent.setup()
    renderAt('/items', '/items', <ItemsPage />)

    await screen.findByTestId('item-1')
    fireEvent.change(screen.getByLabelText('날짜'), { target: { value: '2026-11-10' } })
    await user.selectOptions(screen.getByLabelText('종류'), 'ANNUAL')
    await user.click(screen.getByRole('button', { name: '일정 등록' }))

    await waitFor(() => {
      expect(bodyOf('/api/days/2026-11-10/type', 'PUT')).toEqual({ dayType: 'ANNUAL', confirm: false })
    })
  })

  it('editForm_showsCompletedDateOnlyForDoneItems', async () => {
    const user = userEvent.setup()
    renderAt('/items', '/items', <ItemsPage />)

    await user.click(within(await screen.findByTestId('item-1')).getByRole('button', { name: '수정' }))
    expect(screen.getByLabelText('시작일')).toBeInTheDocument()
    expect(screen.queryByLabelText('완료일')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '취소' }))

    await user.click(screen.getByRole('button', { name: /완료 \(1\)/ }))
    await user.click(within(screen.getByTestId('item-3')).getByRole('button', { name: '수정' }))
    expect(screen.getByLabelText('완료일')).toHaveValue('2026-10-30')
  })

  it('completeButton_changesStatus', async () => {
    const user = userEvent.setup()
    renderAt('/items', '/items', <ItemsPage />)

    const row = await screen.findByTestId('item-1')
    await user.click(within(row).getByRole('button', { name: '완료' }))

    await waitFor(() => {
      expect(bodyOf('/api/work-items/1/status', 'PATCH')).toEqual({ status: 'DONE' })
    })
  })
})
