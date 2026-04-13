package apidocs

import "embed"

//go:embed openapi.json swagger-ui.html
var Files embed.FS
