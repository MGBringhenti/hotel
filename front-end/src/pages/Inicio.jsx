import { Link } from 'react-router-dom'
import { perfis } from '../lib/formatacao.js'
export default function Inicio({ sessao }) {
  const equipe = sessao.funcao !== 'CLIENTE'
  return (
    <>
      <section className="welcome">
        <div>
          <p className="welcome-profile">{perfis[sessao.funcao]}</p>
          <h1>
            {equipe
              ? 'Cada estadia começa com uma boa recepção.'
              : 'Sua próxima estadia começa aqui.'}
          </h1>
          <p>
            {equipe
              ? 'Organize reservas e acomodações. Cuide das pessoas, do planejamento à chegada.'
              : 'Encontre uma acomodação, confira as datas e acompanhe sua reserva.'}
          </p>
          <Link className="button light" to="/disponibilidade">
            {equipe ? 'Criar uma reserva' : 'Planejar minha estadia'}
          </Link>
        </div>
        <div className="welcome-art" aria-hidden="true">
          <div className="key-tag">
            <span>Hotel</span>
            <strong>H</strong>
            <span>Bem-vindo</span>
          </div>
        </div>
      </section>
      <section className="home-links" aria-label="Acesso rápido">
        <Link to="/reservas">
          <span>Reservas</span>
          <h2>{equipe ? 'Acompanhar estadias' : 'Ver minhas reservas'}</h2>
          <p>Datas, valores e situação de cada reserva.</p>
        </Link>
        <Link to="/quartos">
          <span>Acomodações</span>
          <h2>Conhecer os quartos</h2>
          <p>Tipos, capacidade e diárias disponíveis.</p>
        </Link>
        <Link to={equipe ? '/clientes' : '/conta'}>
          <span>{equipe ? 'Atendimento' : 'Seu acesso'}</span>
          <h2>{equipe ? 'Gerenciar clientes' : 'Cuidar da minha conta'}</h2>
          <p>
            {equipe
              ? 'Dados de contato e cadastros de hóspedes.'
              : 'Atualize sua senha quando precisar.'}
          </p>
        </Link>
      </section>
    </>
  )
}
