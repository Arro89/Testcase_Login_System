# Testcase_Login_System
Testcase MicroAA Labs.  Servern körs lokalt just nu mot port 2000 (har ingenstans jag kan sätta upp servern just nu). 
Kan ändras i framtiden för att köras på port 8080 för HTTP requests . Använder ett custom serializable objekt för att skicka data via sockets - kan också bytas till JSON vid behov. 
Lagt ned fokuset på backend snarare än front-end, så en mindre klient skickar data till servern för att testa funktionaliteten. Inget returneras just nu utan printas bara
i serverns terminal för proof of concept, men går att fixa klienten så den har en ObjectInputStream för att ta emot svar från servern. Dokumentation finns i koden. 
