# Réponse audio longue Alexa

Cette intégration évite la limite de réponse conversationnelle d’Alexa. Après
une demande, la Skill répond immédiatement « Ok patron, je lance le job. »,
puis lance un flux `AudioPlayer` MP3. Le Bridge transmet la réponse Hermes au
service Kokoro local, phrase par phrase, et écrit les MP3 dans ce flux.

```text
Echo -> Lambda -> Hermes Bridge -> Hermes Gateway (SSE)
                         |                 |
                         +-> Kokoro TTS <---+
                         |
                         +-> flux MP3 HTTPS AudioPlayer -> Echo
```

## Contrat de sécurité

Le point de lecture MP3 est accessible sans en-tête Bearer, car Alexa ne peut
pas en ajouter. Il est protégé par une capacité opaque, aléatoire, à usage
court, présente uniquement dans l’URL AudioPlayer. Elle est liée à un job et à
l’utilisateur Alexa ayant créé ce job. Elle ne contient ni identité, ni texte,
ni clé Hermes. Les réponses HTTP définissent `Cache-Control: no-store` et
`Referrer-Policy: no-referrer`.

Le proxy public ne doit jamais enregistrer les chaînes de requête de
`/v1/channels/alexa/audio/streams/`, car elles contiennent cette capacité.

Dire « Alexa, stop » ou « Alexa, pause » annule le flux en cours : la Lambda
appelle le Bridge et celui-ci interrompt son abonnement SSE Hermes.

## Mise en place

Fais les étapes dans cet ordre. Le Bridge et Kokoro doivent être déployés avant
de mettre à jour la Lambda.

### 1. Préparer le modèle Alexa

Dans la console Alexa Developer, active l’interface **Audio Player** pour la
skill, puis sauvegarde et reconstruis le modèle français. Le manifest versionné
est `alexa-skill/skill-package/skill.json`.

### 2. Déployer Kokoro sur le réseau interne

L’image publiée est :

```text
ghcr.io/aytronnfr/hermes-kokoro:main-<short-sha>
```

Expose le port interne `8880` seulement. Le Bridge attend par défaut
`http://hermes-kokoro:8880`, qui doit donc correspondre au nom DNS interne du
Service Kubernetes (ou à l’URL interne équivalente). N’expose jamais Kokoro sur
Internet.

La sonde de disponibilité est `GET /health`. Prévois au moins 2 GiB de mémoire
pour le chargement du modèle et un cache de volume si ton runtime le nécessite.

### 3. Configurer le Bridge

Ajoute ces variables à son secret d’exécution, avec les clés déjà présentes :

```dotenv
TTS_BASE_URL=http://hermes-kokoro:8880
TTS_MODEL=kokoro
TTS_VOICE=ff_siwis
```

Le pont reste public pour recevoir Lambda, mais il faut conserver
`HERMES_BRIDGE_API_KEY` : seuls les appels API de la Lambda peuvent créer ou
annuler un job. Le flux MP3 ne dépend que de sa capacité opaque.

### 4. Déployer le Bridge

Déploie une image `ghcr.io/aytronnfr/hermes-bridge:main-<short-sha>` qui
contient l’API audio. Vérifie d’abord `/actuator/health`, puis la résolution DNS
de `TTS_BASE_URL` depuis le pod Bridge.

### 5. Mettre à jour la Lambda

Depuis `alexa-skill/lambda/`, crée l’archive de déploiement :

```powershell
npm run package
```

Charge ensuite `hermes-bridge-alexa.zip` dans la fonction Lambda en conservant :

```dotenv
BRIDGE_URL=https://<domaine-public-du-bridge>
BRIDGE_API_KEY=<même valeur que HERMES_BRIDGE_API_KEY>
```

Le timeout de cette Lambda ne concerne que la création instantanée du job et
reste volontairement court. Le travail long se poursuit dans le flux audio.

### 6. Tester avec une Echo physique

1. « Alexa, ouvre assistant Hermes ».
2. « Envoie quelle est la météo à Paris ».
3. Alexa doit acquitter immédiatement, puis lire la réponse dès que la première
   phrase est synthétisée.
4. Pendant la lecture, dis « Alexa, stop » et vérifie dans les logs Bridge que
   le job est annulé.

Le simulateur Alexa valide la directive mais ne reproduit pas toujours la
lecture AudioPlayer. Le test final doit être fait sur une Echo liée au même
compte de développeur.

## Dépannage

- Alexa annonce une erreur immédiatement : vérifier `BRIDGE_URL`,
  `BRIDGE_API_KEY`, et le statut HTTP de `POST /v1/channels/alexa/audio/jobs`.
- L’acquittement est prononcé mais aucun son ne suit : vérifier que l’URL audio
  est publiquement accessible en HTTPS/443 et que l’Ingress ne tamponne pas la
  réponse MP3.
- Aucun son après plusieurs secondes : vérifier la connectivité Bridge ->
  Gateway, puis Bridge -> Kokoro, et les logs `hermes-bridge` / `hermes-kokoro`.
- Le stop ne coupe pas le travail : vérifier les événements `AudioPlayer` de la
  Lambda et l’appel `POST /v1/channels/alexa/audio/cancel`.
