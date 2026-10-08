import { createBrowserRouter } from 'react-router'
import { CatalogPage } from '../features/catalog/CatalogPage'
import { PokemonDetailPage } from '../features/catalog/PokemonDetailPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { RequireAuth } from '../features/auth/RequireAuth'
import { AppLayout } from './AppLayout'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <CatalogPage /> },
      { path: 'pokemon/:idOrName', element: <PokemonDetailPage /> },
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
