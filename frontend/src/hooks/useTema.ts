import { useCallback, useEffect, useState } from 'react'

type Tema = 'claro' | 'oscuro'

function temaInicial(): Tema {
  return document.documentElement.dataset.tema === 'oscuro' ? 'oscuro' : 'claro'
}

export function useTema() {
  const [tema, setTema] = useState<Tema>(temaInicial)

  useEffect(() => {
    document.documentElement.dataset.tema = tema
    localStorage.setItem('tema', tema)
  }, [tema])

  const alternar = useCallback(() => {
    setTema((t) => (t === 'claro' ? 'oscuro' : 'claro'))
  }, [])

  return { tema, alternar }
}
