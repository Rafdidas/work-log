import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, jsonBody } from '../../api/client'
import type { Profile } from '../../api/types'
import { Card } from '../../components/Card'

const inputClass =
  'h-11 w-full rounded-xl border border-line bg-white px-3 text-[15px] outline-none focus:border-prussian focus:ring-2 focus:ring-prussian/15'

export function LoginPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [mode, setMode] = useState<'login' | 'signup'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')

  const submit = useMutation({
    mutationFn: () =>
      mode === 'login'
        ? api<Profile>('/api/auth/login', jsonBody('POST', { email, password }))
        : api<Profile>('/api/auth/signup', jsonBody('POST', { email, password, name })),
    onSuccess: (profile) => {
      queryClient.setQueryData(['me'], profile)
      navigate('/daily')
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    submit.mutate()
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-4">
      <h1 className="mb-1 text-[28px] font-semibold tracking-tight">Work Log</h1>
      <p className="mb-6 text-sm text-charcoal">일일 업무 기록과 주간 보고 초안</p>
      <Card className="p-5">
        <form onSubmit={onSubmit} className="space-y-3">
          {mode === 'signup' && (
            <input
              className={inputClass}
              placeholder="이름"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          )}
          <input
            className={inputClass}
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            className={inputClass}
            type="password"
            placeholder="비밀번호 (8자 이상)"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required
          />
          {submit.isError && <p className="text-sm text-red-700">{submit.error.message}</p>}
          <button
            type="submit"
            disabled={submit.isPending}
            className="h-11 w-full rounded-xl bg-prussian text-[15px] font-semibold text-white transition-colors hover:bg-prussian/90 disabled:opacity-60"
          >
            {mode === 'login' ? '로그인' : '가입하고 시작'}
          </button>
        </form>
      </Card>
      <button
        type="button"
        onClick={() => {
          submit.reset()
          setMode(mode === 'login' ? 'signup' : 'login')
        }}
        className="mt-4 text-sm text-charcoal hover:text-ink"
      >
        {mode === 'login' ? '계정이 없나요? 가입하기' : '이미 계정이 있나요? 로그인'}
      </button>
    </div>
  )
}
