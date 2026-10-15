package com.fatec.gisa.services.cadastro;

// Cadastro "com acesso" já gravado. emailEnviado = false quando o e-mail com a senha temporária
// não pôde ser enviado: o cadastro continua valendo e o administrador pode reenviar o acesso
public record CadastroComAcesso<T>(T cadastro, boolean emailEnviado) {
}
