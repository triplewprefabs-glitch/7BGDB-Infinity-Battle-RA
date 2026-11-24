# ENERGY OPS · ARCore Android (protótipo isolado)

Este módulo é separado dos protótipos WebXR. Não altera `index.html`, os modos Treino/Fuzileiro Espacial nem as versões v39–v46.

## Objetivo da versão 0.1

Validar se o ARCore mantém a pose local da câmera (posição e orientação) enquanto o aparelho gira e deixa de apontar para o marcador original.

A versão atual:
- inicia uma sessão ARCore;
- lê o estado de tracking e a pose da câmera a cada frame;
- mostra posição e quaternion no referencial local do ARCore;
- não grava nem envia imagens da câmera;
- ainda não faz calibração por imagem, comunicação entre celulares, avatares ou combate.

## Abrir no Android Studio

Abra a pasta `arcore-android` como projeto Gradle. Use JDK 17 e instale Android SDK Platform 35. O projeto usa ARCore SDK `1.45.0`.

No Android, o teste requer:
- aparelho compatível com ARCore;
- Google Play Services for AR instalado/atualizado;
- permissão de câmera concedida;
- boa iluminação e detalhes visuais no ambiente.

## Teste 360°

1. Instale e abra o app nos dois aparelhos, inicialmente testando um de cada vez.
2. Aguarde o estado `TRACKING · pose atualizada`.
3. Mantenha o aparelho em movimento lento e contínuo.
4. Gire 180° e depois 360°, deixando o marcador fora do campo de visão.
5. Observe se a posição e o quaternion continuam mudando e se o estado permanece TRACKING.
6. Repita em uma área com paredes/texturas visíveis e depois em uma área lisa ou pouco iluminada.
7. Copie ou fotografe o diagnóstico e compare os resultados.

## Próxima fase — ainda não implementada

Depois de validar o tracking local, adicionar uma imagem de referência ARCore (Augmented Images) para calibrar o referencial do jogo. Cada celular manterá o tracking visual-inercial local quando a imagem sair de vista; ao reencontrá-la, poderá corrigir a deriva.

Importante: os referenciais locais de dois aparelhos não são automaticamente iguais. Para multiplayer, será necessário calcular e compartilhar a transformação entre o mundo local de cada aparelho e um referencial comum, além de validar qualidade/idade das poses antes de autorizar tiros ou dano.
