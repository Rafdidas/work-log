import { addWeeks, formatDayTitle, mondayOf, weekDays } from './week'

describe('week', () => {
  it('mondayOf returns the Monday of a midweek date', () => {
    expect(mondayOf('2026-11-04')).toBe('2026-11-02')
  })

  it('mondayOf maps Sunday to the Monday before it', () => {
    expect(mondayOf('2026-11-08')).toBe('2026-11-02')
  })

  it('mondayOf keeps a Monday as is', () => {
    expect(mondayOf('2026-11-02')).toBe('2026-11-02')
  })

  it('weekDays returns Monday to Friday', () => {
    const days = weekDays('2026-10-26')
    expect(days).toHaveLength(5)
    expect(days[0]).toBe('2026-10-26')
    expect(days[4]).toBe('2026-10-30')
  })

  it('addWeeks moves across month boundaries', () => {
    expect(addWeeks('2026-10-26', 1)).toBe('2026-11-02')
    expect(addWeeks('2026-11-02', -2)).toBe('2026-10-19')
  })

  it('formatDayTitle writes month, day and weekday in Korean', () => {
    expect(formatDayTitle('2026-11-02')).toBe('11월 2일 월요일')
  })
})
