param(
    [switch]$ConfigurarSomente,
    [switch]$NaoReconstruir
)

$ErrorActionPreference = 'Stop'
$raizHotel = $PSScriptRoot
$arquivoEnv = Join-Path $raizHotel '.env'

function NovoSegredo([int]$bytes) {
    $dados = New-Object byte[] $bytes
    $gerador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $gerador.GetBytes($dados) }
    finally { $gerador.Dispose() }
    return ([BitConverter]::ToString($dados)).Replace('-', '').ToLowerInvariant()
}

if (-not (Test-Path -LiteralPath $arquivoEnv)) {
    $linhas = @(
        '# Configuracao local do Hotel. Nao versionar este arquivo.'
        ('POSTGRES_PASSWORD=' + (NovoSegredo 24))
        ('NOTIFICACOES_CLIENT_SECRET=' + (NovoSegredo 32))
        'ADMIN_INICIAL_LOGIN=admin'
        ('ADMIN_INICIAL_SENHA=' + (NovoSegredo 16))
        'ADMIN_INICIAL_EMAIL=admin@hotel.test'
        'HOTEL_PORT=8080'
        'MAILPIT_PORT=8025'
        'POSTGRES_PORT=5433'
    )
    [System.IO.File]::WriteAllLines($arquivoEnv, $linhas, (New-Object System.Text.UTF8Encoding $false))
    Write-Host 'Configuracao criada. O login inicial e a senha estao no arquivo .env da raiz.'
} else {
    Write-Host 'Configuracao existente preservada.'
}

if ($ConfigurarSomente) { return }

Push-Location $raizHotel
try {
    docker info --format '{{.ServerVersion}}' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Abra o Docker Desktop e aguarde o mecanismo iniciar.' }

    docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Confira os valores do arquivo .env.' }

    if (-not $NaoReconstruir) {
        # Menor consumo de memoria durante a primeira compilacao.
        docker compose --parallel 1 build
        if ($LASTEXITCODE -ne 0) { throw 'A compilacao falhou. Consulte a mensagem acima e execute novamente.' }
    }

    docker compose up -d --wait --wait-timeout 360
    if ($LASTEXITCODE -ne 0) { throw 'Um servico nao iniciou. Consulte: docker compose ps; docker compose logs --tail 80' }

    Write-Host 'Hotel iniciado. Portas padrao: site http://localhost:8080 e Mailpit http://localhost:8025.'
    Write-Host 'Se alterou as portas no .env, use os novos valores.'
    Write-Host 'Login inicial: ADMIN_INICIAL_LOGIN e ADMIN_INICIAL_SENHA no arquivo .env.'
    Write-Host 'Para parar preservando os dados: docker compose down'
} finally {
    Pop-Location
}
