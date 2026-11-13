package br.com.best2bee.hotel_mcp;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class HotelTools {

    private final HotelApiClient api;

    public HotelTools(HotelApiClient api) {
        this.api = api;
    }

    @Tool
    (name = "listar_quartos", description = """
            Lista uma página dos quartos cadastrados no Hotel,
            incluindo número, status, tipo, capacidade e diária.
            A primeira página é zero. Use tamanho 20 por padrão.
            Retorna o total de registros e páginas.
            Para continuar a consulta, incremente pagina até last ser true.
            Inclui quartos de todos os status.
            Esta ferramenta não calcula disponibilidade por período.
            """)
    public HotelApiClient.PaginaResposta<HotelApiClient.QuartoResposta> listarQuartos(
            @ToolParam(description = "Página desejada, começando em zero") int pagina,

            @ToolParam(description = "Quantidade por página, de 1 a 100; use 20 por padrão") int tamanho) {

        return api.listarQuartos(pagina, tamanho);
    }

    @Tool(name = "consultar_reserva_por_id", description = """
            Consulta uma reserva do Hotel pelo seu ID.
            Retorna cliente, quarto, datas, valor total e status.
            """)
    public HotelApiClient.ReservaResposta consultarReserva(
            @ToolParam(description = "ID inteiro positivo da reserva") int id) {

        return api.consultarReserva(id);
    }
}
