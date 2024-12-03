import pandas as pd
import sys
import json
from scipy.sparse import csr_matrix
import pymysql
from CollaborativeFiltering import CollaborativeFiltering

# Step 1: Load Interaction Data from MySQL
def load_interaction_data(host, user, password, database):
    """
    Load user-article interactions from the database.
    :param host: Database host
    :param user: Database user
    :param password: Database password
    :param database: Database name
    :return: DataFrame with columns [user_id, article_id, interaction]
    """
    conn = pymysql.connect(host=host, user=user, password=password, database=database)
    query = "SELECT user_id, article_id, interaction FROM user_article_interactions"
    df = pd.read_sql_query(query, conn)
    conn.close()

    # Ensure interaction data is numeric
    df['interaction'] = pd.to_numeric(df['interaction'], errors='coerce')
    df.dropna(subset=['interaction'], inplace=True)

    return df

# Step 2: Create User-Item Matrix
def create_user_item_matrix(df):
    user_map = {user_id: idx for idx, user_id in enumerate(df['user_id'].unique())}
    article_map = {article_id: idx for idx, article_id in enumerate(df['article_id'].unique())}
    df['user_idx'] = df['user_id'].map(user_map)
    df['article_idx'] = df['article_id'].map(article_map)
    user_item_matrix = csr_matrix((df['interaction'], (df['user_idx'], df['article_idx'])))
    return user_item_matrix, user_map, article_map

# Step 3: Main function to run everything
if __name__ == "__main__":
    # Check if user_id is provided as an argument
    # if len(sys.argv) < 2:
    #     print("Please provide user_id as an argument.")
    #     sys.exit(1)

    # Get user_id from command-line argument
    user_id = 1#int(sys.argv[1])

    # Database credentials
    host = "localhost"
    user = "root"
    password = "amna"
    database = "news_scraper"

    # Load interaction data
    interaction_data = load_interaction_data(host, user, password, database)
    #print("Loaded Interaction Data:")
    #print(interaction_data.head())

    # Create user-item matrix
    user_item_matrix, user_map, article_map = create_user_item_matrix(interaction_data)

    # Initialize Collaborative Filtering
    cf = CollaborativeFiltering(user_item_matrix, user_map, article_map)

    # Compute user similarity
    cf.compute_similarity()

    # Generate recommendations for a user
    recommendations = cf.recommend(user_id, top_n=5)

 # Reverse map article indices to IDs
    reverse_article_map = {idx: article_id for article_id, idx in article_map.items()}
    recommendations = [reverse_article_map[article_idx] for article_idx in recommendations]

    print(json.dumps(recommendations))
