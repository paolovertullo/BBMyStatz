import os

def converti_in_txt_unica_cartella(cartella_origine, cartella_destinazione):
    """
    Naviga la cartella di origine, converte tutti i file in .txt e li mette
    nella cartella di destinazione specificata.
    """

    os.makedirs(cartella_destinazione, exist_ok=True) # Crea la cartella di destinazione se non esiste
    
    for root, _, files in os.walk(cartella_origine):
        for file in files:
            # Calcola il percorso completo del file di origine
            file_path = os.path.join(root, file)

            # Determina il nome del file di destinazione con estensione .txt
            file_name, _ = os.path.splitext(file)
            dest_file = os.path.join(cartella_destinazione, file_name + ".txt")
            
            try:
              # Leggi il contenuto del file sorgente
              with open(file_path, 'r', encoding='utf-8') as infile:
                file_content = infile.read()

              # Scrivi il contenuto nel file di destinazione con estensione .txt
              with open(dest_file, 'w', encoding='utf-8') as outfile:
                outfile.write(file_content)
                print(f"Convertito: {file_path} -> {dest_file}")
            except Exception as e:
              # Gestisci eccezioni (es. file binari, permessi)
              print(f"Impossibile convertire {file_path}: {e}")

if __name__ == "__main__":
    # Esempio di utilizzo
    cartella_origine = input("Inserisci il percorso della cartella di origine: ")
    cartella_destinazione = input("Inserisci il percorso della cartella di destinazione: ")

    if not os.path.isdir(cartella_origine):
        print("Errore: La cartella di origine non esiste.")
    else:
        converti_in_txt_unica_cartella(cartella_origine, cartella_destinazione)
        print("Conversione completata.")