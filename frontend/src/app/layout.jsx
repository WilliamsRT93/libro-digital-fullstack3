import './globals.css'

export const metadata = {
  title: 'Libro de Clases Digital',
  description: 'Plataforma de gestion escolar - DSY1106 Fullstack III DuocUC',
}

export default function RootLayout({ children }) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  )
}
