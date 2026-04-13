package main

import (
	"context"
	"fmt"
	"log"
	"os"

	"github.com/stainless-sdks/rails-go"
)

func main() {
	if os.Getenv("RAILS_API_KEY") == "" {
		log.Fatal("RAILS_API_KEY is required")
	}
	client := rails.NewClient()
	ctx := context.Background()

	id := os.Getenv("RAILS_SAMPLE_ACCOUNT_ID")
	if id == "" {
		fmt.Println("Rails Go SDK client is configured from the environment.")
		fmt.Println("Set RAILS_SAMPLE_ACCOUNT_ID to call Accounts.Get and print the response.")
		return
	}

	res, err := client.Accounts.Get(ctx, id)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%+v\n", res)
}
