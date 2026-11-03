// 날짜는 모두 'YYYY-MM-DD' 문자열로 다룬다. UTC 자정으로 계산해 시간대 영향을 받지 않는다.
const DAY_MS = 86_400_000
const WEEKDAYS_KO = ['일', '월', '화', '수', '목', '금', '토']

function parse(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

function format(date: Date): string {
  return date.toISOString().slice(0, 10)
}

export function addDays(iso: string, days: number): string {
  return format(new Date(parse(iso).getTime() + days * DAY_MS))
}

/** 해당 날짜가 속한 주의 월요일(일요일은 그 전 월요일). */
export function mondayOf(iso: string): string {
  const dow = parse(iso).getUTCDay()
  return addDays(iso, dow === 0 ? -6 : 1 - dow)
}

/** 월요일부터 금요일까지 다섯 날짜. */
export function weekDays(monday: string): string[] {
  return [0, 1, 2, 3, 4].map((i) => addDays(monday, i))
}

export function addWeeks(monday: string, weeks: number): string {
  return addDays(monday, weeks * 7)
}

/** 서울 기준 오늘 날짜. */
export function todaySeoul(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date())
}

export function weekdayKo(iso: string): string {
  return WEEKDAYS_KO[parse(iso).getUTCDay()]
}

/** 예: "11월 2일 월요일" */
export function formatDayTitle(iso: string): string {
  const d = parse(iso)
  return `${d.getUTCMonth() + 1}월 ${d.getUTCDate()}일 ${WEEKDAYS_KO[d.getUTCDay()]}요일`
}

const WEEKDAY_ENUM_KO: Record<string, string> = {
  MONDAY: '월',
  TUESDAY: '화',
  WEDNESDAY: '수',
  THURSDAY: '목',
  FRIDAY: '금',
  SATURDAY: '토',
  SUNDAY: '일',
}

export function weekdayEnumKo(day: string): string {
  return WEEKDAY_ENUM_KO[day] ?? day
}

/** 서버 보고서와 같은 표기: 다섯 요일이면 "월~금", 아니면 "월·화·수". */
export function formatDays(days: string[]): string {
  return days.length === 5 ? '월~금' : days.map(weekdayEnumKo).join('·')
}

/** 예: "11월 2일" */
export function formatMonthDay(iso: string): string {
  const d = parse(iso)
  return `${d.getUTCMonth() + 1}월 ${d.getUTCDate()}일`
}
