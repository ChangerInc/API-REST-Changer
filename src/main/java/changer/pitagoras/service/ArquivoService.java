package changer.pitagoras.service;

import changer.pitagoras.model.Arquivo;
import changer.pitagoras.model.Circulo;
import changer.pitagoras.model.Usuario;
import changer.pitagoras.repository.ArquivoRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractQueue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ArquivoService {
    @Autowired
    private ArquivoRepository repository;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private CirculoService circuloService;
    @Getter
    private String extensaoAux;
    @Getter
    private String nomeAux;

    public Arquivo encontrarArq(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID não fornecido");
        }

        Arquivo arq = repository.findByIdArquivo(id).orElse(null);

        if (arq == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado");
        }

        return arq;
    }

    public void separarExtensao(String nomeDocumento) {
        StringBuilder extensao = new StringBuilder();
        StringBuilder nome = new StringBuilder();

        boolean ponto = false;
        for (int i = 0; i < nomeDocumento.length(); i++) {
            char charAtual = nomeDocumento.charAt(i);

            if (charAtual == '.') {
                ponto = true;
            }

            if (!ponto) {
                nome.append(charAtual);
            }

            if (ponto && charAtual != '.') {
                extensao.append(charAtual);
            }
        }

        nomeAux = nome.toString();
        extensaoAux = extensao.toString();
    }

    public Arquivo salvar(Arquivo arq) {
        return repository.save(arq);
    }

    public Arquivo salvar(MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        }

        separarExtensao(file.getOriginalFilename());
        Arquivo arquivo = new Arquivo(
                nomeAux,
                BigDecimal.valueOf(file.getSize()),
                extensaoAux
        );

        try {
            arquivo.setBytesArquivo(file.getResource().getContentAsByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return repository.save(arquivo);
    }

    public Arquivo salvar(UUID codigo, MultipartFile file) {
        Usuario usuario = usuarioService.encontrarUsuario(codigo);
        Arquivo arquivo = salvar(file);

        usuario.getArquivos().add(arquivo);
        usuarioService.salvarUser(usuario);

        return arquivo;
    }

    public Arquivo salvar(UUID codigo, Arquivo arq) {
        Usuario usuario = usuarioService.encontrarUsuario(codigo);
        Arquivo arquivo = salvar(arq);

        usuario.getArquivos().add(arquivo);
        usuarioService.salvarUser(usuario);

        return arquivo;
    }

    public Boolean deletarArquivo(UUID codigo, UUID idArquivo) {
        if (codigo == null || idArquivo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Usuario user = usuarioService.encontrarUsuario(codigo);
        if (user == null) {
            return false;
        }

        Arquivo arq = buscarArquivo(idArquivo);
        if (arq == null) {
            return false;
        }

        user.getArquivos().remove(arq);
        usuarioService.salvarUser(user);
        return true;
    }

    public Boolean adicionarArquivoNoGrupo(UUID idCirculo, UUID idArquivo) {
        Arquivo arquivo = encontrarArq(idArquivo);
        Optional<Circulo> circulo = circuloService.(idCirculo);

        if (circulo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Círculo não encontrado");
        }

        circulo.get().getArquivos().add(arquivo);
        circuloRepository.save(circulo.get());
        return true;
    }

    public Arquivo buscarArquivo(UUID id) {
        return repository.findByIdArquivo(id).orElse(null);
    }

    public List<Arquivo> resgatarArquivos(UUID id, Boolean user) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informações faltando");
        }

        return user ? usuarioService.pegarArq(id) : circuloService.resgatarArquivos(id);
    }

    public byte[] pegarArquivo(UUID id) {
        return encontrarArq(id).getBytesArquivo();
    }
}
