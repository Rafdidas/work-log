// 백엔드 DTO와 같은 모양. 날짜는 'YYYY-MM-DD', 달은 'YYYY-MM' 문자열이다.

export type DayType = 'WORK' | 'HALF_AM' | 'HALF_PM' | 'ANNUAL' | 'HOLIDAY'
export type ItemStatus = 'WAITING' | 'IN_PROGRESS' | 'DONE'
export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export const DAY_TYPE_LABEL: Record<DayType, string> = {
  WORK: '근무',
  HALF_AM: '오전 반차',
  HALF_PM: '오후 반차',
  ANNUAL: '연차',
  HOLIDAY: '휴일',
}

export const STATUS_LABEL: Record<ItemStatus, string> = {
  WAITING: '대기',
  IN_PROGRESS: '진행 중',
  DONE: '완료',
}

export interface Profile {
  id: number
  email: string
  name: string
  teamLabel: string
}

export interface WorkItem {
  id: number
  title: string
  tag: string | null
  status: ItemStatus
  recurring: boolean
  showInPlan: boolean
  sortOrder: number
  startedAt: string | null
  completedAt: string | null
  totalMd: number
}

export interface EntryView {
  itemId: number
  title: string
  tag: string | null
  status: ItemStatus
  recurring: boolean
  weight: number
  md: number
  memo: string | null
  completedBefore: boolean
}

export interface WaitingItemView {
  itemId: number
  title: string
  tag: string | null
}

export interface DayView {
  date: string
  dayType: DayType
  dayTypeLabel: string
  workMd: number
  memo: string | null
  entries: EntryView[]
  waitingItems: WaitingItemView[]
}

export interface DaySummary {
  date: string
  dayType: DayType
  totalMd: number
  hasEntries: boolean
}

export interface LeaveView {
  date: string
  dayType: DayType
  dayTypeLabel: string
}

export interface PlanItem {
  itemId: number
  title: string
  tag: string | null
}

export interface AchievedItem {
  itemId: number
  title: string
  days: Weekday[]
  md: number
  completed: boolean
  memos: { day: Weekday; memo: string }[]
}

export interface WeeklyReport {
  title: string
  todo: PlanItem[]
  achieved: AchievedItem[]
  leaves: { date: string; type: DayType }[]
  next: PlanItem[]
  text: string
}

export interface EffortReport {
  label: string
  from: string
  to: string
  months: { month: string; md: number }[]
  total: number
  text: string
}

export interface MonthlySummary {
  month: string
  rows: { tag: string | null; title: string; md: number; ratioPercent: number }[]
  workDays: number
  leaveCounts: Partial<Record<DayType, number>>
  totalMd: number
  tsv: string
}
