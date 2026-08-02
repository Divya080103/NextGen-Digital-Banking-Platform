from fastapi import FastAPI

app = FastAPI(
    title="NextGen Banking — AI Loan Intelligence Service",
    description="Microservice providing credit risk evaluation, scoring, and repayment probability predictions.",
    version="1.0.0"
)

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "ai-loan-service",
        "modelVersion": "v1.0-rules"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
