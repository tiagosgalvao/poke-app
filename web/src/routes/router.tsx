import { createBrowserRouter } from 'react-router'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { RequireAuth } from '../features/auth/RequireAuth'
import { AppLayout } from './AppLayout'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <p className="text-slate-600">Pokemon catalog coming soon.</p> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      {
        path: 'my-pokedex',
        element: (
          <RequireAuth>
            <p className="text-slate-600">Your local Pokedex is coming soon.</p>
          </RequireAuth>
        ),
      },
    ],
  },
])
