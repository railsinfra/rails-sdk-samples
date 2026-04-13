module github.com/rails/sdk-samples/go

go 1.22

require github.com/stainless-sdks/rails-go v0.0.0

require (
	github.com/tidwall/gjson v1.18.0 // indirect
	github.com/tidwall/match v1.1.1 // indirect
	github.com/tidwall/pretty v1.2.1 // indirect
	github.com/tidwall/sjson v1.2.5 // indirect
)

replace github.com/stainless-sdks/rails-go => ../../mvp/rails-sdks/sdks/rails-go
