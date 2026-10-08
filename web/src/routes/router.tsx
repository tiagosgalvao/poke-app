import { createBrowserRouter } from 'react-router'
import { AppLayout } from './AppLayout'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <p className="text-slate-600">Pokemon catalog coming soon.</p>,
      },
    ],
  },
])
